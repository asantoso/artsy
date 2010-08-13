DROP TABLE IF EXISTS requests;
CREATE TABLE requests (
	requestId INTEGER PRIMARY KEY AUTOINCREMENT,
	requestName TEXT,
	requestType INTEGER,
	requestString TEXT,
	callId INTEGER,
	httpMethod TEXT,
	response TEXT,
	status INTEGER
);
	