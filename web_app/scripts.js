var messages = [
	'⇄', 'A', 'B', 'C', 'D', '➟', '🔊&#xFE0E;', '↺'
];

// Initialize Firebase
var config = {
	apiKey: "AIzaSyCWYRgBALYoIjZgn1hT7lQbfnfVeqDwEo8",
	databaseURL: "https://androbuzz-8d0b1.firebaseio.com/"
};
firebase.initializeApp(config);
var database = firebase.database();
var clientId;
var clientJson;
var server_ping = 1;
var firebase_ping = 1;
var start_time = 1;

var clientRef = database.ref('clientId');

clientRef.on('value', function(snapshot) {
	if ( snapshot.val() !== null ) {
		clientJson = snapshot.val();
		clientId = clientJson['clientId'];
	}
});

function changePhone() {
	console.log( 'Changing phone...' );
}

$( 'document' ).ready( function() {
	var view = {};
	var messageList = $( '#consoleDiv' );
	var mv = $( '#main-view' );

	function getPhones() {
		var request = {
			'action': 'getRegisteredDevices',
		};
		$.ajax( {
			type: 'POST',
			dataType: 'json',
			url: 'functions.php',
			data: request,
			success: function( data ) {
				// here we create the dropdown options
				var dropdownHTML = '';
				var totalPhones = data.length;
				for ( var i = 0; i < totalPhones; i++ ) {
					dropdownHTML += '<li><a href=\"javascript:changePhone(\''+data[i]['reg_id']+'\');">' + data[i]['name'] + ' &bull; ' + data[i]['number'] + '</a></li>';
				}
				// Get phone NAME instead of manufacturer and model
				view['registered-phones'] = dropdownHTML;
				redraw();
			}
		} );

	}

	getPhones();

	function redraw( field, value ) {
		var windowHeight = $( window ).height();
		$( '#main-view' ).css( 'max-height', windowHeight + 'px' );

		if ( field === undefined || value === undefined ) {
			mv.find( '[data-label]' ).each( function() {
				$( this ).html( view[$( this ).attr( 'data-label' )] );
			} );
			mv.find( '[data-title]' ).each( function() {
				$( this ).attr( 'title', view[$( this ).attr( 'data-title' )] );
			} );
		} else {
			$( '#main-view' ).find( "[data-label=" + field + "]" ).html( value );
		}
	}

	$( '.action' ).click( function() {
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
		start_time = Date.now();

		// Prepare data
		var json = { "message": message, "clientId": clientId };
		var request = {
			'action': 'androbuzz',
			'message': JSON.stringify(json)
		};
		request = $( this ).serialize() + '&' + $.param( request );

		// Send to PHP
		$.ajax( {
			type: 'POST',
			dataType: 'json',
			url: 'functions.php',
			data: request,
			success: function( data ) {
				var msg = JSON.parse(data['response'].substring(1, data['response'].length-1));
				total_ping = Date.now() - start_time;
				firebase_ping = data['firebase_ping'];
				server_ping = total_ping - firebase_ping;
				view['ping'] = (server_ping) + 'ms + ' + data['firebase_ping'] + 'ms';

				var msgId = data['messageId'];
				var msgId = data['battery'];
				view['response'] = msgId;
				view['status'] = '<strong>' + message + '</strong> sent. Waiting for delivery confirmation...';
				view['client_id'] = data['clientId'];
				if ( clientJson !== null ) {
					view['device'] = clientJson['brand'] + ' ' + clientJson['model'] + '&nbsp;&nbsp;&bull;&nbsp;&nbsp;' + clientJson['number'];
				}

				view['message'] = '<span class="action_pill panel">' + messages[data['message']] + '</span>&nbsp;<span id=timer>0</span> ';
				seconds = 0;

				view['message_id'] = 'msgId: ' + msgId;

				var dataref = database.ref('messages/' + data['messageId']);

				var timeOut = setTimeout(function(){
					dataref.off();
					redraw( 'status', 'Operation timed out. Ready.' );
				}, 15000 );


				dataref.on('value', function(snapshot) {
					if ( snapshot.val() !== null ) {
						messageList.append( '<span class="bzzz' + message + '">' + messages[message] + '</span>' ).on( 'click', 'span', function() {
							$( this ).animate( { width: 0 }, function() {
								$( this ).remove();
							} );
						} );
						//view['battery-level'] = ?????????????????????['battery'] + '%';
						messageList.find( 'span:last-child' ).attr( 'title', timestamp );
						device_ping = Date.now() - start_time - server_ping;
						redraw( 'ping', $( '[data-label=ping]' ).html() + ' + ' + device_ping + 'ms' );
						redraw( 'status', 'Ready' );
						$( '.action' ).blur();
						clearTimeout(timeOut);
						dataref.off();
					}
				});

				redraw();
			}
		} );
		return false;
	} );
} );
var seconds = 0;
var timer = setInterval( function() {
	if ( seconds < 60 )
		$( '#timer' ).html( seconds + 's ago' );
	else
		$( '#timer' ).html( 'over 1 minute ago' );
	++seconds;
}, 1000 );

$( window ).on( 'load', function() {
	setTimeout( function() {
		$( "#NOP" ).click();
	}, 1000 ); // Test connection and initialize view
	$( "#CLEAR" ).on( 'click', function() {
		$( '#consoleDiv' ).find( 'span' ).slideUp( "normal", function() {
			$( this ).remove();
		} );
	} );
} );