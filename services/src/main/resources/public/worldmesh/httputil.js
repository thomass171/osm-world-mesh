
async function doGet(url, jsonHandler) {
    try {
      const response = await fetch(url, { method: 'GET'});
      var json = await response.json();
      if (response.ok) {
          jsonHandler(json);
      } else {
          const responseError = {
              statusText: response.statusText,
              status: response.status,
              body: json
          };
          throw responseError;
      }
    } catch (error) {
        console.log('There was an error', error);
        if (error.status == 400) {
            showMessage(error.body.error);
        }
    }
}

async function doGetBlob(url, blobHandler) {
    try {
      const response = await fetch(url, { method: 'GET'});
      if (response.ok) {
          var blob = await response.blob();
          blobHandler(blob);
      } else {
          console.log('There was an error loading blob from ' + url, response.status);
      }
    } catch (error) {
        console.log('There was an error', error);
    }
}

async function doPost(url, jsonHandler, data) {
    try {
        const response = await fetch(url, {
            method: 'POST',
            //mode: 'cors', // no-cors, *cors, same-origin
            cache: 'no-cache', // *default, no-cache, reload, force-cache, only-if-cached
            //credentials: 'same-origin', // include, *same-origin, omit
            headers: {
               'Content-Type': 'application/json'
            },
            //redirect: 'follow', // manual, *follow, error
            //referrerPolicy: 'no-referrer', // no-referrer, *no-referrer-when-downgrade, origin, origin-when-cross-origin, same-origin, strict-origin, strict-origin-when-cross-origin, unsafe-url
            body: JSON.stringify(data) // body data type must match "Content-Type" header
       });
       processFetchResponse(response, jsonHandler);
    } catch (error) {
        // only reached on network error?
        console.log('There was an error', error);
    }
}

async function httpDelete(url, jsonHandler) {
    try {
        const response = await fetch(url, {
            method: 'DELETE',
            //mode: 'cors', // no-cors, *cors, same-origin
            cache: 'no-cache', // *default, no-cache, reload, force-cache, only-if-cached
            //credentials: 'same-origin', // include, *same-origin, omit
            headers: {
            },
            //redirect: 'follow', // manual, *follow, error
            //referrerPolicy: 'no-referrer', // no-referrer, *no-referrer-when-downgrade, origin, origin-when-cross-origin, same-origin, strict-origin, strict-origin-when-cross-origin, unsafe-url
       });
       processFetchResponse(response, jsonHandler);
    } catch (error) {
        // only reached on network error?
        console.log('There was an error', error);
    }
}

async function httpPut(url, jsonHandler, data) {
    try {
        const response = await fetch(url, {
            method: 'PUT',
            //mode: 'cors', // no-cors, *cors, same-origin
            cache: 'no-cache', // *default, no-cache, reload, force-cache, only-if-cached
            //credentials: 'same-origin', // include, *same-origin, omit
            headers: {
            },
            //redirect: 'follow', // manual, *follow, error
            //no json, either XML or a plain file name
             body: data
       });
       processFetchResponse(response, jsonHandler);
    } catch (error) {
        // only reached on network error?
        console.log('There was an error', error);
    }
}

async function processFetchResponse(response, jsonHandler) {
var json;
      try {
         json = await response.json();
      } catch(e) {
         json = null;
      }
      if (response.ok) {
          if (jsonHandler != null) {
              jsonHandler(json);
          }
      } else {
          /*const responseError = {
              statusText: response.statusText,
              status: response.status,
              body: json
          };
          //throw responseError;*/
          if (response.status == 400 && json != null) {
              showMessage(json.error);
          }
      }
}