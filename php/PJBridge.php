<?php

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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
class PJBridge
{
    private $sock;
    private $jdbc_enc;
    private $app_enc;
    private $conID;
    public $last_search_length = 0;
    public function __construct(string $host = 'localhost', int $port = 4444, string $jdbc_enc = 'ascii', string $app_enc = 'ascii')
    {
        $this->sock = fsockopen($host, $port);
        $this->jdbc_enc = $jdbc_enc;
        $this->app_enc = $app_enc;
        $this->conID = NULL;
    }

    public function __destruct()
    {
        fclose($this->sock);
    }

    private function parse_reply()
    {
        $il = explode(' ', fgets($this->sock));
        $ol = [];
        foreach ($il as $value)
            $ol[] = iconv($this->jdbc_enc, $this->app_enc, base64_decode($value));
        return $ol;
    }

    private function exchange(array $cmd_a)
    {
        $cmd_s = '';
        foreach ($cmd_a as $tok)
            $cmd_s .= base64_encode(iconv($this->app_enc, $this->jdbc_enc, $tok)) . ' ';
        $cmd_s = substr($cmd_s, 0, - 1) . "\n";
        fwrite($this->sock, $cmd_s);
        return $this->parse_reply();
    }

    public function connect(string $url, string $user, string $pass)
    {
        if ($this->conID === NULL) {
            $reply = $this->exchange([
                'connect',
                $url,
                $user,
                $pass
            ]);
            if ($reply[0] === 'ok') {
                $this->conID = $reply[1];
                return $this->conID;
            }
            return false;
        } else {
            throw new Exception('Ya se conecto a la base.');
        }
    }

    public function exec(string $query)
    {
        $cmd_a = [
            'exec',
            $this->conID,
            $query
        ];
        if (func_num_args() > 1) {
            $args = func_get_args();
            for ($i = 1; $i < func_num_args(); ++ $i)
                $cmd_a[] = $args[$i];
        }
        $reply = $this->exchange($cmd_a);
        if ($reply[0] === 'ok')
            return $reply[1];
        return false;
    }

    public function fetch_array(string $res)
    {
        $reply = $this->exchange([
            'fetch_array',
            $this->conID,
            $res
        ]);
        if ($reply[0] === 'ok') {
            $row = [];
            for ($i = 0; $i < $reply[1]; ++ $i) {
                $col = $this->parse_reply($this->sock);
                $row[$col[0]] = $col[1];
            }
            return $row;
        }
        return false;
    }

    public function free_result(string $res)
    {
        $reply = $this->exchange([
            'free_result',
            $this->conID,
            $res
        ]);
        if ($reply[0] === 'ok')
            return true;
        return false;
    }

    public function close()
    {
        $reply = $this->exchange([
            'close',
            $this->conID
        ]);
        if ($reply[0] === 'ok') {
            $this->conID = NULL;
            return true;
        }
        return false;
    }
    public function numRows(string $res)
    {
        $reply = $this->exchange(['num_rows', $this->conID,$res]);
        if ($reply[0] === 'ok') {
            return $reply[1];
        }
        return -1;
    }
    public function commit():bool
    {
        $reply = $this->exchange([
            'commit',
            $this->conID
        ]);
        var_dump($reply);
        if ($reply[0] === 'ok') {
            return true;
        }
        return false;
    }
    public function rollback():bool
    {
        $reply = $this->exchange([
            'rollback',
            $this->conID
        ]);
        if ($reply[0] === 'ok') {
            return true;
        }
        return false;
    }
    public function setAutoCommit(bool $autoCommit):bool
    {
        $reply = $this->exchange([
            'setAutoCommit',
            $this->conID,$autoCommit? 'true' : 'false'
        ]);
        if ($reply[0] === 'ok') {
            return true;
        }
        return false;
    }
}