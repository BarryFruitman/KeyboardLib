<?php


    // DEVELOPMENT
    /*
	$server_addr = "localhost";
	$username = "k42163_ls";
	$password = "";
	$db_name = "k42163_ls";
	*/

	// AWS PRODUCTION
	$db_host = "license.chiny36l7igk.us-west-1.rds.amazonaws.com";
	$db_port = "3306";
	$db_username = "typesmart";
	$db_password = "";
	$db_name = "license";


    // PRODUCTION
// 	$server_addr = "mysql01.edikon.net";
// 	$username = "cometin_license";
// 	$password = "";
// 	$db_name = "cometin_typesmart_license";
	
	$app_dir = "/hsphere/local/home/cometinc/cometapps.com/typesmart/license";
	
	$auth_password = "HANDSET KEYBOARD";
	$api_key = "$*DFKL*(B451471281A425490439CC52C08930C2JD";

	$trial_expire_timeout = (60 * 60 * 24) * 30;		// 30 days
	$never_expire_timeout = (60 * 60 * 24) * 365 * 10;	// 10 years
?>