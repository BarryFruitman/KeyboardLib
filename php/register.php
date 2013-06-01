<?php

include_once('constant.inc');

$db_conn = mysql_connect($db_host . ':' . $db_port, $db_username, $db_password) or die ('Error connecting to mysql: ' .  mysql_error());
createDatabaseIfNotExist($db_conn, $db_name);
mysql_select_db($db_name);


$xml_writer = new XMLWriter();
$xml_writer->openMemory();
$xml_writer->startElement("license");


// Check Username and Password
$client_api_key = $_POST['api_key'];
$product	  = $_POST['product'];
if($product == '')
	$product = 'typesmart';
$app_store	  = $_POST['app_store'];
$device_id	  = $_POST['device_id'];
$google_id	  = $_POST['google_id'];
$product_type   = $_POST['product_type'];
$coupon_code   	= $_POST['coupon_code'];
$app_version   	= $_POST['app_version'];
$device_type   	= $_POST['device_type'];
$os_version   	= $_POST['os_version'];
$referrer		= $_POST['referrer'];

$ip_address = $_SERVER['HTTP_X_FORWARDED_FOR']?$_SERVER['HTTP_X_FORWARDED_FOR']:$_SERVER['REMOTE_ADDR'];
if(strpos($ip_address,',') !== false) {
	$ip_address = substr($ip_address,0,strpos($ip_address,','));
}

$current_date = date('Y-m-d H:i:s');
$current_time = strtotime($current_date);

// Response params
$status = 'fail';
$error_message = '';
$remained_time = '-1';

if ($client_api_key == $api_key && !empty($google_id) && $google_id != "") {
	// Check for registered device
	$query = "SELECT * FROM register WHERE device_id='" . $device_id . "' AND device_id!='0' AND product='" . $product . "' AND product_type='" . $product_type . "'";
	$table_data = mysql_query($query) or die(mysql_error());

	if (mysql_num_rows($table_data) > 1) {
		// Multiple device ids match! Use a more specific query
		$query = "SELECT * FROM register WHERE device_id='" . $device_id . "' AND device_id!='0' AND google_id='" . $google_id . "' AND google_id!='' AND product='" . $product . "' AND product_type='" . $product_type . "' LIMIT 1";
		$table_data = mysql_query($query) or die(mysql_error());
	}

	if (mysql_num_rows($table_data) == 0) {
		// Not registered. Create a new license.
		
		// Result status is always success for new registrations
		$status = 'success';

		// Set appropriate expiry
  		if(isPaidProduct($product_type))
			$expire_timeout = $never_expire_timeout;
 		else
 			$expire_timeout = $trial_expire_timeout;
		$remained_time = $expire_timeout;
		$expiry_date = date('Y-m-d H:i:s', $current_time + $expire_timeout);

		// Insert a new license record
		$query = "INSERT INTO register (product, device_id, device_type, os_version, google_id, app_version, ip_address, registered_date, last_login_date, product_type, app_store, expiry_date, referrer)
		VALUES ('" . 
		$product . "', '" .
		$device_id . "', '" .
		$device_type . "', '" .
		$os_version . "', '" .
		$google_id . "', '" .
		$app_version . "', '" .
		$ip_address . "', '" .
		$current_date . "', '" .
		$current_date . "', '" .
		$product_type . "', '".
		$app_store . "', '".
		$expiry_date . "', '".
		$referrer . "')";

		mysql_query($query);
	} else {
		// Already registered
		while($info = mysql_fetch_array( $table_data )) {
  			if(isPaidProduct($product_type))
  				// Paid products never expire
				$expiry_date = $current_time + $never_expire_timeout;
			else if($product_type == 'trial' && (substr($info['app_version'], 3) == '1.0' || substr($info['app_version'], 3) == '0.9'))
				// Reset trial for users upgrading from v1.0
				$expiry_date = $current_time + $trial_expire_timeout;
			else {
				// Keep old expiry date
				$expiry_date = strtotime($info['expiry_date']);
			}
			
			// Compute remining time
			$remained_time = $expiry_date - $current_time;

			// Increment login count
			$register_count_incr = $info['register_count'] + 1;

			// Check remaining time and set result status
			if ($remained_time >= 0) {
				$status = 'success';
			} else {
				$status = 'fail';
				$error_message = 'App expired';
			}

			// Don't overwrite a non-blank referrer with a blank
			if($info['referrer'] != "" && $referrer == '')
				$referrer = $info['referrer'];

			// Fetch the unique ID for this row
			$id = $info['ID'];

			// Update the license record
			$update = "UPDATE register SET " .
					"expiry_date='" .  date('Y-m-d H:i:s', $expiry_date) . "'," .
					"device_type='" . $device_type . "'," .
					"last_login_date='" . $current_date . "'," .
					"app_version='" . $app_version . "'," .
					"os_version='" . $os_version . "'," .
					"ip_address='" . $ip_address . "'," .
					"app_store='" . $app_store . "'," .
					"referrer='" . $referrer . "'," .
					"register_count=" . $register_count_incr .
					" WHERE ID='" . $id . "'";
				
			mysql_query($update);
		}
	}


	// Apply coupon
	if(!empty($coupon_code) && $coupon_code != "") {
		// Check if coupon is valid.
		$coupon_query = "SELECT * FROM coupons WHERE (code='" . $coupon_code."')";
		$coupon_table_data = mysql_query($coupon_query) or die(mysql_error());
			
		$coupon_info = mysql_fetch_array( $coupon_table_data );
		if ($coupon_info){
			if( $coupon_info['device_id'] && $coupon_info['device_id']!="") {
				// device_id is already set
				$status = 'fail';
				$error_message = 'Code was already used';
			} else {
				// Coupon was found
				$coupon_expiry_date = strtotime($coupon_info['expiry_date']);
				if($coupon_expiry_date  > $current_time) {
					// Coupon is not expired
					$status = 'success';
					$error_message = '';

					// Set license expiry
					$coupon_expiry_date = date('Y-m-d H:i:s', $coupon_expiry_date);
					$expiry_date = strtotime($coupon_expiry_date);
					$remained_time = $expiry_date - $current_time;

					// Update database
					mysql_query("UPDATE register SET expiry_date='".$coupon_expiry_date."' WHERE device_id='".$device_id."'");
					mysql_query("UPDATE coupons SET device_id='".$device_id."' WHERE code='".$coupon_code."'");
				} else {
					// Coupon is expired
					$status = 'fail';
					$error_message = 'Code is expired';
				}
			}
		} else {
			// Coupon not found
			$status = 'fail';
			$error_message = 'Code not found';
		}
	}
} else {
	// api_key mismatch
	$status = 'fail';
	$remained_time = '-1';
	$error_message = 'Request from invalid client';
}

// Assign response params
$xml_writer->writeElement('status', $status);
$xml_writer->writeElement('remained_time', $remained_time);
$xml_writer->writeElement('error', $error_message);
$xml_writer->writeElement('referrer', $referrer);

// Close XML tag
$xml_writer->endElement();
mysql_close($db_conn);

// Send response to client
print $xml_writer->outputMemory();

function isPaidProduct($product_type) {
	if($product_type == 'paid' || $product_type == 'upgrade')
		return TRUE;
	
	return FALSE;
}

function errorEnd($xml_writer, $conn){
	$xml_writer->endElement();
	print $xml_writer->outputMemory();
	mysql_close($conn);
	die;
}

function createDatabaseIfNotExist($conn, $db_name) {
	// Create database
	if (mysql_query("CREATE DATABASE IF NOT EXISTS " . $db_name, $conn)) {
	} else {
	}

	// Create table
	mysql_select_db($db_name, $conn);

	if (!table_exists('register', $db_name)) {
		mysql_query("CREATE TABLE `register` (
				`ID` int(11) NOT NULL AUTO_INCREMENT,
				`product` varchar(16) NOT NULL DEFAULT 'typesmart',
				`device_id` varchar(32) DEFAULT NULL,
				`device_type` varchar(32) DEFAULT NULL,
				`os_version` varchar(32) DEFAULT NULL,
				`google_id` varchar(128) DEFAULT NULL,
				`app_version` varchar(16) DEFAULT NULL,
				`ip_address` varchar(16) NOT NULL,
				`registered_date` datetime DEFAULT NULL,
				`expiry_date` datetime DEFAULT NULL,
				`last_login_date` datetime DEFAULT NULL,
				`product_type` varchar(16) DEFAULT NULL,
				`app_store` varchar(16) DEFAULT '',
				`referrer` varchar(256) DEFAULT '',
				`register_count` int(11) NOT NULL DEFAULT '1',
				PRIMARY KEY (`ID`)
		) ENGINE=InnoDB  DEFAULT CHARSET=utf8;", $conn
		);
	}

	if (!table_exists('coupons', $db_name)) {
		mysql_query("CREATE TABLE IF NOT EXISTS `coupons` (
				`code` varchar(16) CHARACTER SET latin1 COLLATE latin1_general_ci NOT NULL,
				`expiry_date` datetime NOT NULL,
				`device_id` varchar(32) CHARACTER SET utf8 DEFAULT NULL,
				`comments` text NOT NULL,
				PRIMARY KEY (`code`)
		) ENGINE=MyISAM DEFAULT CHARSET=latin1;", $conn
		);
	}
}

function table_exists ($table, $db) {
	$tables = mysql_list_tables ($db);

	while (list ($temp) = mysql_fetch_array ($tables)) {
		if ($temp == $table) {
			return TRUE;
		}
	}

	return FALSE;
}

function check_field($table, $db, $field) {
	$fields = mysql_list_fields ($db, $table);

	while (list ($temp) = mysql_fetch_array ($fields)) {
		if ($temp == $field) {
			return TRUE;
		}
	}

	return FALSE;
}
?>
