/*
 * Copyright (C) 2007 lenny@mondogrigio.cjb.net
 *
 * This file is part of PJBS (http://sourceforge.net/projects/pjbs)
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Hashtable;

/**
 * This class is instatiated by the ServerThread class. Contains the commands
 * implementations.
 * 
 * @author lenny
 */
public class ServerCommands {
	private ServerThread serverThread;
	private Hashtable<String, Connection> conections = new Hashtable<String, Connection>();
	private Hashtable<String, ResultSet> results = new Hashtable<String, ResultSet>();

	private Connection getConnection(String[] cmd) {
		Connection conn = null;
		if (cmd.length >= 2) {
			conn = conections.get(cmd[1]);
		}
		return conn;
	}

	/** Creates a new instance of ServerCommands */
	public ServerCommands(ServerThread serverThread) {
		this.serverThread = serverThread;
	}

	/**
	 * Connect to a JDBC data source.
	 * 
	 * @param cmd
	 */
	public void connect(String[] cmd) {
		Utils.log("debug", "connect", cmd);
		if (cmd.length == 4) {
			try {
				Connection conn = DriverManager.getConnection(cmd[1], cmd[2], cmd[3]);
				String id = Utils.makeCUID();
				conections.put(id, conn);
				serverThread.write("ok", id);
			} catch (SQLException ex) {
				Utils.log("error", "SQL exception encountered: " + ex.getMessage());
				serverThread.write("ex");
			}
		} else {
			Utils.log("error", "Unexpected error encountered");
			serverThread.write("err");
		}
	}

	/**
	 * Execute an SQL query.
	 * 
	 * @param cmd
	 */
	public void exec(String[] cmd) {
		Utils.log("debug", "exec", cmd);
		Connection conn = getConnection(cmd);
		if (conn != null && cmd.length >= 3) {
			try {
				PreparedStatement st = conn.prepareStatement(cmd[2],
                        ResultSet.TYPE_SCROLL_INSENSITIVE,
                        ResultSet.CONCUR_UPDATABLE);
				st.setFetchSize(1);
				for (int i = 3; i < cmd.length; i++) {
					try {
						st.setDouble(i - 1, Double.parseDouble(cmd[i]));
					} catch (NumberFormatException e) {
						st.setString(i - 1, cmd[i]);
					}
				}
				if (st.execute()) {
					String id = Utils.makeUID();
					results.put(id, st.getResultSet());
					serverThread.write("ok", id);
				} else {
					serverThread.write("ok", st.getUpdateCount());
				}
			} catch (SQLException ex) {
				Utils.log("error", "SQL exception encountered: " + ex.getMessage());
				serverThread.write("ex");
			}
		} else {
			Utils.log("error", "Unexpected error encountered");
			serverThread.write("err");
		}
	}

	/**
	 * Fetch a row from a ResultSet.
	 * 
	 * @param cmd
	 */
	public void fetch_array(String[] cmd) {
		Utils.log("debug", "fetch_array", cmd);
		Connection conn = getConnection(cmd);
		if (conn != null && cmd.length == 3) {
			ResultSet rs = (ResultSet) results.get(cmd[2]);
			if (rs != null) {
				try {
					if (rs.next()) {
						ResultSetMetaData rsmd = rs.getMetaData();
						int cn = rsmd.getColumnCount();
						serverThread.write("ok", cn);
						for (int i = 1; i <= cn; i++) {
							serverThread.write(rsmd.getColumnName(i), rs.getString(i));
						}
					} else {
						serverThread.write("end");
					}
				} catch (SQLException ex) {
					Utils.log("error", "SQL exception encountered: " + ex.getMessage());
					serverThread.write("ex");
				}
			} else {
				Utils.log("error", "Unexpected error encountered");
				serverThread.write("err");
			}

		} else {

			Utils.log("error", "Unexpected error encountered");
			serverThread.write("err");
		}
	}

	/**
	 * Fetch a row from a ResultSet.
	 * 
	 * @param cmd
	 */
	public void numRows(String[] cmd) {
		Utils.log("debug", "numRows", cmd);
		Connection conn = getConnection(cmd);
		if (conn != null && cmd.length == 3) {
			ResultSet rs = (ResultSet) results.get(cmd[2]);
			if (rs == null) {
				Utils.log("error", "Unexpected error encountered");
				serverThread.write("err");
			} else {
				try {
					int actual = rs.getRow();
					rs.last();
					int row = rs.getRow();
					rs.relative(-(row - actual));
					serverThread.write("ok", row);

				} catch (SQLException ex) {
					Utils.log("error", "SQL exception encountered: " + ex.getMessage());
					serverThread.write("ex");
				}
			}
		} else {
			Utils.log("error", "Unexpected error encountered:");
			serverThread.write("err");
		}
	}

	/**
	 * Release a ResultSet.
	 * 
	 * @param cmd
	 */
	public void free_result(String[] cmd) {
		Utils.log("debug", "free_result", cmd);
		Connection conn = getConnection(cmd);
		if (conn != null && cmd.length == 3) {
			ResultSet rs = (ResultSet) results.get(cmd[2]);
			if (rs != null) {
				results.remove(cmd[2]);
				try {
					rs.close();
					serverThread.write("ok");
				} catch (SQLException e) {
					serverThread.write("exx");
					Utils.log("error", "Unexpected error encountered: "+ e.getMessage());
				}
			} else {
				Utils.log("error", "Unexpected error encountered");
				serverThread.write("err");
			}
		} else {
			Utils.log("error", "Unexpected error encountered");
			serverThread.write("err");
		}
	}

	/**
	 * Release the JDBC connection.
	 */
	public void close(String[] cmd) {
		Utils.log("debug", "close", cmd);
		Connection conn = getConnection(cmd);
		if (conn == null){serverThread.write("err");}
		else
		{
			try {
				conn.close();
				serverThread.write("ok");
			} catch (SQLException ex) {
				Utils.log("error", "could not close JDBC connection");
				serverThread.write("exx");
			}
		}
	}

	public void columns(String[] cmd) {
		Utils.log("debug", "columns", cmd);
		Connection conn = getConnection(cmd);
		if (conn != null && cmd.length == 3) {
			ResultSet rs = (ResultSet) results.get(cmd[2]);
			if (rs == null) {
				Utils.log("error", "Unexpected error encountered");
				serverThread.write("err");
			} else {
				try {
					ResultSetMetaData rsmd = rs.getMetaData();
					int cn = rsmd.getColumnCount();
					serverThread.write("ok", cn);
					for (int i = 1; i <= cn; i++) {
						serverThread.write(rsmd.getColumnName(i), rsmd.getColumnLabel(i));
					}
				} catch (SQLException ex) {
					Utils.log("error", "SQL exception encountered: " + ex.getMessage());
					serverThread.write("ex");
				}
			}
		} else {
			Utils.log("error", "Unexpected error encountered");
			serverThread.write("err");
		}
	}

	/**
	 * rollback
	 * 
	 * @param cmd
	 */
	public void rollback(String[] cmd) {
		Utils.log("debug", "rollback", cmd);
		Connection conn = getConnection(cmd);
		if (conn != null && cmd.length >= 2) {
			try {
				conn.rollback();
				serverThread.write("ok");
			} catch (SQLException ex) {
				Utils.log("error", "SQL exception encountered: " + ex.getMessage());
				serverThread.write("ex");
			}
		} else {
			Utils.log("error", "Unexpected error encountered");
			serverThread.write("err");
		}
	}
	public void commit(String[] cmd) {
		Utils.log("debug", "commit", cmd);
		Connection conn = getConnection(cmd);
		if (conn != null && cmd.length >= 2) {
			try {
				conn.commit();
				serverThread.write("ok");
			} catch (SQLException ex) {
				Utils.log("error", "SQL exception encountered: " + ex.getMessage());
				serverThread.write("ex");
			}
		} else {
			Utils.log("error", "Unexpected error encountered");
			serverThread.write("err");
		}
		
	}

	public void setAutoCommit(String[] cmd) {
		Utils.log("debug", "setAutoCommit", cmd);
		Connection conn = getConnection(cmd);
		if (conn != null && cmd.length >= 2) {
			try {
				boolean ok=true;
				if(cmd.length>=3) {
					ok=Boolean.parseBoolean(cmd[2]);
				}
				conn.setAutoCommit(ok);
				serverThread.write("ok");
			} catch (SQLException ex) {
				Utils.log("error", "SQL exception encountered: " + ex.getMessage());
				serverThread.write("ex");
			}
		} else {
			Utils.log("error", "Unexpected error encountered");
			serverThread.write("err");
		}
		
	}
}