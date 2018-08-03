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
	require_once __DIR__ . '/firebase.php';

	$firebase = new Firebase();
	$clientsJson = $firebase->getJson('clients');
	$clients = json_decode($clientsJson, true);
	$devices = []; // all devices in the database
	$i = 0;
	foreach($clients as $key => $value){
		$devices[$i] = [
			"reg_id" => $clients[$key]['clientId'],
			"model"  => $clients[$key]['model'],
			"number" => $clients[$key]['number'],
			"name"   => $clients[$key]['brand'], // Please store phone name too: Settings > General > About Phone > Name
		];
		$i++;
	}
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

