<?php
require "PJBridge.php";

$dbHost = "localhost";
$dbName = "postgres";
$dbPort = "5432";
$dbUser = "postgres";
$dbPass = "";
jdbc:postgresql://server-name:server-port/database-name


$connStr = "jdbc:postgresql://${dbHost}:${dbPort}/${dbName}";


/*$db2 = new PJBridge();
$result2 = $db2->connect($connStr, $dbUser, $dbPass);
$db2->setAutoCommit(false);
$cursor2 = $db2->exec("insert into desa72.prueba (descripcion)values('autocommit1 false commit')");
print_r("commit ".$db2->commit());
$cursor2 = $db2->exec("insert into desa72.prueba (descripcion)values('autocommit1 false rollback')");
print_r("commit ".$db2->rollback());
$cursor2 = $db2->exec("insert into desa72.prueba (descripcion)values('autocommit2 false commit')");
print_r("commit ".$db2->commit());
$db2->close();

$db3 = new PJBridge();
$result3 = $db3->connect($connStr, $dbUser, $dbPass);
$db3->setAutoCommit(true);
$cursor3 = $db3->exec("insert into desa72.prueba (descripcion)values('autocommit1 true')");
$db3->close();*/

$db = new PJBridge();
$result = $db->connect($connStr, $dbUser, $dbPass);
if(!$result){
    die("Failed to connect\n");
}
$cursor = $db->exec('SELECT * FROM desa72.prueba');

print_r($db->columns($cursor));
while($db->fetch_row($cursor)){
    print_r("id: " .$db->result($cursor,"id")."\n");
    print_r("descripcion: ".$db->result($cursor,"descripcion")."\n");
    
}
$db->free_result($cursor);
//$db->close();

//$db2->close();


exit;
?>