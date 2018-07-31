<?php
/**
 * Created by PhpStorm.
 * User: Felix
 * Date: 7/30/2018
 * Time: 11:36 PM
 */


require_once __DIR__ . '/config.php';

function send_to_device( $msgId, $message, $clientId ) {


	$regId = $clientId;

	require_once __DIR__ . '/firebase.php';
	require_once __DIR__ . '/push.php';

	$firebase = new Firebase();
	$push     = new Push();

	// notification title
	$title = isset( $_GET[ 'title' ] ) ? $_GET[ 'title' ] : '';

	// notification message
	$message = isset( $message ) ? $message : '';

	$push->setTitle( $title );
	$push->setMessage( $message );
	$push->setImage( '' );
	$push->setIsBackground( FALSE );
	$push->setMsgId( $msgId );

	$json     = $push->getPush();
	$response = $firebase->send( $regId, $json );

	return $response;
}

function androbuzz() {
	$data                      = $_POST;
	$encoded_message           = $data[ "message" ];
	$message                   = json_decode( $encoded_message );
	$start_time                = microtime( TRUE );
	$uniqueId                  = md5( uniqid( 'msg_', TRUE ) );
	$response                  = send_to_device( $uniqueId, $message->message, $message->clientId );
	$end_time                  = microtime( TRUE );
	$return[ "firebase_ping" ] = round( ( $end_time - $start_time ) * 1000 );
	$return[ "response" ]      = json_encode( json_decode( $response )->results );
	$return[ "message" ]       = $message->message;
	$return[ "clientId" ]      = $message->clientId;
	$return[ "messageId" ]     = $uniqueId;
	echo json_encode( $return );
}

// This function will retrieve a list of registered devices
function getRegisteredDevices() {
	$devices = []; // all devices in the database

	// Demo data for testing
	$devices[ 0 ] = [
		"reg_id" => "dwZ4ktuEVNw:APA91bFNsFR6glr9mnlTvQdR0Tijw2PJfH1zPWpnYdksNdX-91voQy5hEJV5SWmnAvaJ4hOifvrFLYrc0VufLosERB-ZukszxTKMvEHxgYq3Yq6rs0Qk8y10U5Rbged9V6CI8BucqWk4",
		"model"  => "LGE LG-M150",
		"number" => "773-111-1234",
		"name"   => "Phoenix 3", // Please store phone name too: Settings > General > About Phone > Name
	];
	$devices[ 1 ] = [
		"reg_id" => "dlrCBea7mgc:APA91bH9Z1c6EvalxKSEgJxhSlPvTy_0Z3pLRSyEEJuZXLo2pxh6U7vBFP2dJGzUOoKMrCbtFB9Pcn0p2YZt_mT5v-KWwHOzJw_5Sip8E9CdlKOv-xLQCe-soTAEF8kYNqOSxeAGgeaOkVnyo6Qd9f-Jlvnk2jRVYA",
		"model"  => "Lenovo Moto G5",
		"number" => "773-222-1234",
		"name"   => "My Moto",
	];
	$devices[ 2 ] = [
		"reg_id" => "cGmuOsvR3Tk:APA91bElBLlc93lk5GMWQGkTNYMqSJVS-UEYWqbTUCnT3h070sPXWkzI61K_TF_i63RdSNAZ78FEVjTCCVkThxUGl_fxpbBAp8LcDBnMnGnrNAYvjp_V4YHusFx4Nj27gw4ZEOMJXYBU",
		"model"  => "ZTE Z981",
		"number" => "773-333-1234",
		"name"   => "ZTE Maven",
	];

	$return = $devices;

	echo json_encode( $return );
}

// This function will be executed when the select element changes
function getCurrentDevice() {
	$current_device = [
		"reg_id" => "dwZ4ktuEVNw:APA91bFNsFR6glr9mnlTvQdR0Tijw2PJfH1zPWpnYdksNdX-91voQy5hEJV5SWmnAvaJ4hOifvrFLYrc0VufLosERB-ZukszxTKMvEHxgYq3Yq6rs0Qk8y10U5Rbged9V6CI8BucqWk4",
		"model"  => "LGE LG-M150",
		"number" => "773-555-1234",
	];

	$return = $current_device;

	echo json_encode( $return );
}

// This function will be executed when the select element changes
function changeCurrentDevice() {
	$data           = $_POST;
	$encoded_device = $data[ "device" ];
	$device         = json_decode( $encoded_device );

	// Set in firebase the new default device that will be receiving messages.
	// e.g. set_device()

}


function is_ajax() {
	return isset( $_SERVER[ "HTTP_X_REQUESTED_WITH" ] ) && strtolower( $_SERVER[ "HTTP_X_REQUESTED_WITH" ] ) == "xmlhttprequest";
}

if ( is_ajax() ) {
	if ( isset( $_POST[ "action" ] ) && ! empty( $_POST[ "action" ] ) ) { //Checks if action value exists
		$action = $_POST[ "action" ];
		$action();
		die();
	}
}

