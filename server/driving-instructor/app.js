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
var jsdom = require('jsdom');
var jq = 'https://code.jquery.com/jquery-2.2.1.js';

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
			   time:Number,
			   notable:Boolean}]
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

app.get('/videoTest', function(req, res){
	var gridfs = app.get('gridfs');
	var id = currentLesson.lesson;
	var fileLocation = './public/videos/video' + id + '.mp4';
	console.log("video test page accessed");
	gridfs.exist({_id : id}, function(err, found){
		if(err) console.error(err);
		if(found){
			console.log('file found in database');
			var fs_writestream = fs.createWriteStream(fileLocation);
			var readstream = gridfs.createReadStream({
				_id : id
			});
			readstream.pipe(fs_writestream);
			fs_writestream.on('close', function () {
				console.log('file has been written fully!');
				jsdom.env({
					html:fs.readFileSync("./public/videoTest.html", "utf-8"),
					scripts: ['https://code.jquery.com/jquery-2.2.1.js'],
					done: function (err, window) {
					if(err) console.error(err);
					var $ = window.jQuery;
					var $body = $('body');
					var $source = $body.find('source');
					$source.attr('src', '/videos/video' + id + '.mp4');
					res.send(window.document.documentElement.outerHTML);
				}
				});
			});
		};
	});
});

app.get('/video', function(req, res){
	var gridfs = app.get('gridfs');
	var HTMLString = '';
	console.log('video page accessed');
	
	Lesson.find({}, function(err, lessons){
		if (err) console.error(err);
		lessons.forEach(function(lesson){
			console.log(lesson.lesson);
		});
		
		jsdom.env({
			html:fs.readFileSync("./public/videoList.html", "utf-8"),
			scripts: ['https://code.jquery.com/jquery-2.2.1.js'],
			done: function (err, window) {
				if(err) console.error(err);
				var $ = window.jQuery;
				var $buttons = $('body').find('.buttons');
		
				lessons.forEach(function(lesson, index, array){
					gridfs.files.findOne({_id : lesson.lesson}, function(err, file){
						if(err) console.error(err);
						if(file){
							console.log(file.filename);
							$buttons.after('<p><button type="button" class="btn btn-default">' + file.filename + '</button></p>');
							console.log($buttons.html());
							if(index === array.length - 1){
								res.send(window.document.documentElement.outerHTML);
							}
						};
					});
				});
			}
		});
	});
});

app.post('/upload', upload.single('video'), function (req, res) {
	var locations = JSON.parse(req.body.JSON).lesson[0].locations;
	var sensors = JSON.parse(req.body.JSON).lesson[1].sensors;
	var gridfs = app.get('gridfs');
	
	var writestream = gridfs.createWriteStream({
		filename: convertDate() + '.mp4',
		mode:'w',
		content_type:'video/mp4'
	});
	fs.createReadStream(req.file.path).pipe(writestream);
	
	writestream.on('close', function (file) {
		res.send('Thank you for uploading!');
		console.log(sensors);
		console.log(locations);
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
										time: sensors[i].time,
										notable: sensors[i].notable});
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

function convertDate(){
	var d = new Date();
	return d.toString();
}

module.exports = app;
