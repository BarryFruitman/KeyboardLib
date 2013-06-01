<?php
$error_id = htmlspecialchars($_POST["error_id"]);
// <HACK>
if(strpos($stack_trace, "showKey(") !== false) {
	$error_id = "showKey";
}
// </HACK>
$stack_trace = htmlspecialchars($_POST["stack_trace"]);
$details = htmlspecialchars($_POST["details"]);
$device_id = htmlspecialchars($_POST["device_id"]);
$google_id = htmlspecialchars($_POST["google_id"]);
$os_ver = htmlspecialchars($_POST["os_ver"]);
$app_ver = htmlspecialchars($_POST["app_ver"]);
$screen = htmlspecialchars($_POST["screen"]);
$enabled_imes = htmlspecialchars($_POST["enabled_imes"]);
$heap_size = htmlspecialchars($_POST["heap_size"]);
$heap_free = htmlspecialchars($_POST["heap_free"]);
$heap_used = htmlspecialchars($_POST["heap_used"]);
$meta_error = htmlspecialchars($_POST["meta_error"]);
print "-=-=-=-=-=-=-=-=-=-=-";
print strftime("%c");
print_r($_POST);
$logFile = "/hsphere/local/home/cometinc/cometapps.com/typesmart/reports/error_report-" . $app_ver . ".log";
$fh = fopen($logFile, "a") or die("can't open file");
fwrite($fh, "***************************************************************************************************************************************\n");
fwrite($fh, "***************************************************************************************************************************************\n");
fwrite($fh, "** " . strftime("%c") . "\n");
fwrite($fh, "** Error #" . $error_id . "\n");
if($meta_error != "")
	fwrite($fh, "** Meta error   : " . $meta_error . "\n");
fwrite($fh, "** Device ID    : " . $device_id . "\n");
fwrite($fh, "** Google ID    : " . $google_id . "\n");
fwrite($fh, "** IP address   : " . $_SERVER["REMOTE_ADDR"] . "\n");
fwrite($fh, "** App version  : " . $app_ver . "\n");
fwrite($fh, "** OS version   : " . $os_ver . "\n");
fwrite($fh, "** Screen       : " . $screen . "\n");
fwrite($fh, "** Heap size    : " . $heap_size . "\n");
fwrite($fh, "** Heap free    : " . $heap_free . "\n");
fwrite($fh, "** Heap used    : " . $heap_used . "\n");
fwrite($fh, "** Enabled IMEs : " . $enabled_imes . "\n");
fwrite($fh, "** Stack trace  : " . $stack_trace . "\n");
fwrite($fh, "** Details      : " . $details . "\n");
fwrite($fh, "***************************************************************************************************************************************\n");
if($error_id == "getBitmapDrawable") {
	$image_path = htmlspecialchars($_POST["image_path"]);
	fwrite($fh, "** image_path = " . $image_path . "\n");
}
if($error_id == "loadKeyboardThemesDrawable") {
	$ndrawable = htmlspecialchars($_POST["ndrawable"]);
	fwrite($fh, "** ndrawable = " . $ndrawable . "\n");
}
if($error_id == "OutOfMemory") {
	$width = htmlspecialchars($_POST["width"]);
	$height = htmlspecialchars($_POST["height"]);
	$buffer_count = htmlspecialchars($_POST["buffer_count"]);
	fwrite($fh, "** width = " . $width . "\n");
	fwrite($fh, "** height = " . $height . "\n");
	fwrite($fh, "** buffer_count = " . $buffer_count . "\n");
}
if($error_id == "showKey") {
	$x = htmlspecialchars($_POST["x"]);
	$y = htmlspecialchars($_POST["y"]);
	fwrite($fh, "** x = " . $x . "\n");
	fwrite($fh, "** y = " . $y . "\n");
}
if($error_id == "login") {
	$response = htmlspecialchars($_POST["response"]);
	fwrite($fh, "** response = " . $response . "\n");
}
fwrite($fh, "***************************************************************************************************************************************\n");
fwrite($fh, "Class detail:  \n");
$prefix = "class_";
foreach ($_POST as $key => $value) {
	if(strlen($key) >= strlen($prefix) && substr_compare($key, $prefix, 0, strlen($prefix)) == 0) {
		fwrite($fh, "    " . $key . " = " . $value . "\n");
	}
}
fwrite($fh, "----------------------------------------------------\n");
fwrite($fh, "Shared prefs:  \n");
$prefix = "keyboard_prefs_";
foreach ($_POST as $key => $value) {
	if(strlen($key) >= strlen($prefix) && substr_compare($key, $prefix, 0, strlen($prefix)) == 0) {
		fwrite($fh, "    " . $key . " = " . $value . "\n");
	}
}
fwrite($fh, "----------------------------------------------------\n");
fwrite($fh, "Processes:  \n");
$prefix = "process_";
foreach ($_POST as $key => $value) {
	if(strlen($key) >= strlen($prefix) && substr_compare($key, $prefix, 0, strlen($prefix)) == 0) {
		fwrite($fh, "    " . $key . " = " . $value . "\n");
	}
}
fwrite($fh, "----------------------------------------------------\n");
fwrite($fh, "Installed packages:  \n");
$prefix = "package_";
foreach ($_POST as $key => $value) {
	if(strlen($key) >= strlen($prefix) && substr_compare($key, $prefix, 0, strlen($prefix)) == 0) {
		fwrite($fh, "    " . $key . " = " . $value . "\n");
	}
}
fwrite($fh, "----------------------------------------------------\n");
fwrite($fh, "Call trace:  \n");
$prefix = "trace_";
foreach ($_POST as $key => $value) {
	if(strlen($key) >= strlen($prefix) && substr_compare($key, $prefix, 0, strlen($prefix)) == 0) {
		fwrite($fh, "    " . $key . " = " . $value . "\n");
	}
}
fclose($fh);?>