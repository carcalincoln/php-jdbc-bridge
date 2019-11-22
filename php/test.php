<?php
require "PJBridge.php";


$tiempo_inicial = microtime(true); //true es para que sea calculado en segundos
$dbHost = "draco";
$dbName = "desa72";
$dbPort = "7200";
$dbUser = "CUENTA2";
$dbPass = "CUENTA2";

$connStr = "jdbc:adabasd://${dbHost}:${dbPort}/${dbName}";

$db = new PJBridge();
$result = $db->connect($connStr, $dbUser, $dbPass);
if(!$result){
    die("Failed to connect\n");
}

$idOrganismo=2;
if ($_GET['organismo']){
	$idOrganismo=$_GET['organismo'];
}
$sql='select * from cuenta2adm.verif where ID_ORGANISMO='.$idOrganismo;
print($sql."<br>");
$cursor = $db->exec($sql);
if($cursor){
	$cant=0;
	print("<table><thead><th>ID</th><th>NRO_TIMBRADO</th><th>ITEM_NUME</th><th>TIPO_CREDITO</th><th>ID_ORGANISMO</th><th>CUIT_CUIL</th><th>FECHA_ACRED</th><th>FECHA_DIST</th><th>FACTU_NUME</th><th>IDTIPOCREDITO</th></thead><tbody>");
	while($row = $db->fetch_array($cursor)){
		$cant++;
		print("<tr>");
		foreach ($row as $clave => $valor){
			print("<td>".$valor."</td>");
		}
		print("</tr>");
	}
	$db->free_result($cursor);
	print_r("</tbody></table><br>Cantidad de registros: ".$cant);
}
else{
	print("Error");
}
$tiempo_final = microtime(true);
$tiempo = $tiempo_final - $tiempo_inicial; 
echo "<br>El tiempo de ejecución del archivo ha sido de " . $tiempo . " segundos";
$db->close();
?>
