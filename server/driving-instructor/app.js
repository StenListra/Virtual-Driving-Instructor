var express = require('express');
var path = require('path');
var favicon = require('serve-favicon');
var logger = require('morgan');
var cookieParser = require('cookie-parser');
var bodyParser = require('body-parser');
var multer = require('multer');
var mongoose = require('mongoose');
var grid = require('gridfs-stream');
var fs = require('fs');

var app = express();
var storage = multer.diskStorage({
	destination: function (req, file, cb) {
		cb(null, './uploads')
	},
	filename: function (req, file, cb) {
		if(file.fieldname === 'video'){
			cb(null, file.fieldname + '-' + Date.now() + '.mp4');
		}
	}
})

grid.mongo = mongoose.mongo;
mongoose.connect('mongodb://localhost:27017/test');

var conn = mongoose.connection;
conn.on('error', console.error.bind(console, 'connection error:'));
conn.once('open', function(){
	var gfs = grid(conn.db);
	app.set('gridfs', gfs);
	console.log("Database connection successful");
});

app.set('mongoose', mongoose);

var lessonSchema = mongoose.Schema({
	lesson: mongoose.Schema.Types.ObjectId,
	locations: [{longitude:Number,
				 latitude:Number,
				 speed:Number,
				 time:Number}],
	sensors: [{x:Number,
			   y:Number,
			   z:Number,
			   time:Number}]
});

var Lesson = mongoose.model('Lesson',lessonSchema);
var currentLesson = new Lesson();
var upload = multer({storage:storage});
var video;
// view engine setup
app.set('views', path.join(__dirname, 'views'));
app.set('view engine', 'jade');

app.use(logger('dev'));
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: false }));
app.use(cookieParser());
app.use(express.static(path.join(__dirname, 'public')));

app.get('/testPage', function(req, res){
	var gridfs = app.get('gridfs');
	console.log("upload page accessed");
	gridfs.exist({_id : currentLesson.lesson}, function(err, found){
		if(err) console.error(err);
		found ? console.log("File: exists" + _id) : console.log("File does not exist");
	}
	res.sendFile(__dirname + "/" + "public/testPage.html");
});

app.get('/videoTest', function(req, res){
	console.log("video test page accessed");
	res.sendFile(__dirname + "/" + "public/videoTest.html");
});

app.post('/upload', upload.single('video'), function (req, res) {
	var locations = JSON.parse(req.body.JSON).lesson[0].locations;
	var sensors = JSON.parse(req.body.JSON).lesson[1].sensors;
	var gridfs = app.get('gridfs');
	
	var writestream = gridfs.createWriteStream({
		filename:req.file.fieldname,
		mode:'w',
		content_type:req.file.mimetype
	});
	fs.createReadStream(req.file.path).pipe(writestream);
	
	writestream.on('close', function (file) {
		res.send('Thank you for uploading!');
		
		currentLesson.lesson = file._id;

  		for(var i = 0; i<locations.length; i++){
			currentLesson.locations.push({longitude: locations[i].longitude, 
										  latitude: locations[i].latitude,
										  speed: locations[i].speed,
										  time: locations[i].time});
		}
		
		for(var i = 0; i<sensors.length; i++){
			currentLesson.sensors.push({x: sensors[i].x,
										y: sensors[i].y,
										z: sensors[i].z,
										time: sensors[i].time});
		}
		
		currentLesson.save(function (err) {
			if (err) console.error(err);
		});
		
        fs.unlink(req.file.path, function (err) {
          if (err) console.error("Error: " + err);
          console.log('successfully deleted : '+ req.file.path );
        });
    });
});

// catch 404 and forward to error handler
app.use(function(req, res, next) {
  var err = new Error('Not Found');
  err.status = 404;
  next(err);
});

app.use(function(err, req, res, next) {
	console.log(err);
  res.status(err.status || 500);
  res.render('error', {
    message: err.message,
    error: err
  });
});

module.exports = app;
