# DriveGallery
This abandonned Android app project was based around the [Google Drive Android API](https://developers.google.com/drive/android/).

The API allows access to a user's Google Drive files and folder. However there is a significant constraint to this access:

_The Android Drive API only works with the https://www.googleapis.com/auth/drive.file scope. This means that only files which a user has opened or created with your application can be matched by a query._

I keep this repository as the code still gives me examples of how to perform some basic tasks with the API:
* connect the API client to the server
* pick a file
* pick a folder
* read the metadata of a file
* read the content of a file
