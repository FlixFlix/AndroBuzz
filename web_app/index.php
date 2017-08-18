<?php
function send_to_device( $message ) {

	$regId = 'f_qDcchkCVc:APA91bExbJQ1hdVQ8gQyAUDOEq-CLHrtRmEz4W37XZ70OO_ghFdXNib5DVvJUbGP-AZSR62KM9X_OYg6hPyMvm_ZPs1ZffwZDa8WmuOquRLtJyOfkJLNqLCCKaUB55orj4q0TJzmN27i';

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

	$json     = $push->getPush();
	$response = $firebase->send( $regId, $json );

	return $response;
}

function androbuzz() {
	$data                    = $_POST;
	$message                 = $data[ "message" ];
	$start_time              = microtime( TRUE );
	$response                = send_to_device( $message );
	$end_time                = microtime( TRUE );
	$return[ "device_ping" ] = round( ( $end_time - $start_time ) * 1000 );
	$return[ "response" ]    = json_encode( json_decode( $response )->results );
	$return[ "message" ]     = $data[ "message" ];
	echo json_encode( $return );
}

function is_ajax() {
	return isset( $_SERVER[ "HTTP_X_REQUESTED_WITH" ] ) && strtolower( $_SERVER[ "HTTP_X_REQUESTED_WITH" ] ) == "xmlhttprequest";
}

if ( is_ajax() ) {
	if ( isset( $_POST[ "action" ] ) && ! empty( $_POST[ "action" ] ) ) { //Checks if action value exists
		$action = $_POST[ "action" ];
		if ( $action == "androbuzz" ) {
			androbuzz();
		}
		die();
	}
} ?>
<!DOCTYPE html>
<html lang="en">
<head>
    <script src="https://ajax.googleapis.com/ajax/libs/jquery/3.2.1/jquery.min.js"></script>
    <script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js"></script>
    <script>
		$( 'document' ).ready( function() {
			var view = {};

			hashCode = function(str){
				var hash = 0;
				if (str.length === 0) return hash;
				for (i = 0; i < str.length; i++) {
					char = str.charCodeAt(i);
					hash = ((hash<<5)-hash)+char;
					hash = hash & hash; // Convert to 32bit integer
				}
				return hash;
			};

			function redraw( field, value ) {
				if ( field === undefined || value === undefined ) {
					$( '#main-view' ).find( '[data-label]' ).each( function() {
						$( this ).html( view[$( this ).attr( 'data-label' )] );
					} )
				} else {
					$( '#main-view' ).find( "[data-label=" + field + "]" ).html( value );
				}
			}

			$( 'button' ).click( function() {
				var message = $( this ).attr( 'name' );
				redraw( 'status', 'Sending message... ' + message );
				var currentTime = new Date(),
					hours = currentTime.getHours(),
					minutes = currentTime.getMinutes();
				seconds = currentTime.getSeconds();
				if ( minutes < 10 ) {
					minutes = "0" + minutes;
				}
				if ( seconds < 10 ) {
					seconds = "0" + seconds;
				}
				var timestamp = hours + ':' + minutes + ':' + seconds;
				var start_time = new Date().getTime();
				var data = {
					'action': 'androbuzz',
					'message': message
				};
				data = $( this ).serialize() + '&' + $.param( data );
				$.ajax( {
					type: 'POST',
					dataType: 'json',
					url: '',
					data: data,
					success: function( data ) {
						view['ping'] = (new Date().getTime() - start_time - data['device_ping']) + 'ms';
						view['device_ping'] = data['device_ping'] + 'ms';
						view['message'] = '<strong>' + data['message'] + '</strong> at ' + timestamp;
						var msg = JSON.parse(data['response'].substring(1, data['response'].length-1));
						var msgId = msg['message_id'];
						view['response'] = hashCode(msgId);
						view['status'] = 'Ready';
						redraw();
					}
				} );
				return false;
			} );
			//$( '[name=action6]' ).click(); // Doing this on first page load
		} );
    </script>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Androbuzz!</title>
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css">
    <link href="https://fonts.googleapis.com/css?family=Roboto+Condensed:400,700" rel="stylesheet">
    <style>
        body {
            margin: 20px auto;
            font-family: 'Roboto Condensed', sans-serif;
            background: url(intercom-bg.png);
        }
        * {
            transition: 0.125s
        }
        .container-fluid {
            max-width: 500px;
        }

        #main-view {
            height: calc(100vh - 40px);
            max-width: 500px;
            position: relative;
        }

        .btn-lg {
            padding: 25px 5px;
            line-height: 1.25em;
        }

        .btn {
            margin-top: 10px !important;
        }

        .btn-group > .btn-group:not(:last-child) > .btn {
            border-right: none;
        }

        /*.float-bottom {*/
            /*position: absolute;*/
            /*bottom: 0;*/
            /*width: calc(100% - 30px)*/
        /*}*/

        .panel {
            margin-bottom: 10px;
        }

        table {
            width: 100%;
        }

        td {
            padding: 5px 0;
        }

        td:first-child {
            font-weight: bold;
        }

        td:last-child {
            text-align: right;
        }
    </style>
</head>
<body>

<div class="container-fluid">
    <div class="row">
        <div id="main-view" class="col-md-12">
            <div class="panel panel-primary">
                <div class="panel-heading">
                    <h3 class="panel-title">
                        Status Panel
                    </h3>
                </div>
                <div class="panel-body">
                    <table class="info-list">
                        <tbody>
                        <tr>
                            <td>Last message</td>
                            <td data-label="message"></td>
                        </tr>
                        <tr>
                            <td>FCM response hash</td>
                            <td data-label="response"></td>
                        </tr>
                        <tr>
                            <td>Server ping</td>
                            <td data-label="ping"></td>
                        <tr>
                            <td>FCM ping</td>
                            <td data-label="device_ping"></td>
                        </tr>
                        </tbody>
                    </table>
                </div>
                <div class="panel-footer">
                    <strong>Status:</strong> <span data-label="status"></span>
                </div>
            </div>
                    <button name="1" type="button" class="btn btn-block btn-lg btn-primary">A</button>
                    <button name="2" type="button" class="btn btn-block btn-lg btn-primary">B</button>
                    <button name="3" type="button" class="btn btn-block btn-lg btn-primary">C</button>
<!--                    <button name="4" type="button" class="btn btn-block btn-lg btn-default">D</button>-->
            <div class="float-bottom btn-group btn-group-justified">
                <div class="btn-group">
                    <button name="5" type="button" class="btn btn-lg  btn-default">
                        <i class="glyphicon glyphicon glyphicon-forward"></i>
                    </button>
                </div>
                <div class="btn-group">
                    <button name="6" type="button" class="btn btn-lg  btn-default">
                        <i class="glyphicon glyphicon glyphicon-headphones"></i>
                    </button>
                </div>
                <div class="btn-group">
                    <button name="4" type="button" class="btn btn-lg btn-default">
                        <i class="glyphicon glyphicon-transfer"></i>
                    </button>
                </div>
            </div>
        </div>
    </div>
</div>

</body>
</html>
