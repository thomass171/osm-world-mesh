/**
 * JS related to worldmesh.html
 */

// The host where the scene is launched, not yet in use. This is not the worldmesh-services host.
var host = "XX";
// The host of worldmesh-services. Can be customized with query param, eg. '?serviceshost=http://localhost:8080'
var serviceshost = "https://ubuntu-server.udehlavj1efjeuqv.myfritz.net";

var wellKnownAirports = [
  "EDDK", // Cologne
  "EDKB", // Hangelar
  "EGPF", // Glasgow
  "EGPH", // Edinburgh
  "EHAM", // Schiphol
  "EHTX", // Texel
  "EHLE", // Lelystad
  ];

var allAirports = new Map();

var map;
var tileGroup = null;
var foundGeoRoute = null;
var map_routeMarker = null;
var currentMesh = null;

// rectangular area selection on the map
var selectAreaMode = false;
var selectionStart = null;
var selectionRectangle = null;
var selectionImageOverlay = null;

/**
 *
 */
function initMap() {
    $("#album_title").html("Karte");

    // Just an arbirary start center
    var center = new L.latLng(52.0,7.2);
    console.log("center",center);

    setCss("map", "height", "680px");
    var zoom = 13;
    map = L.map('map').setView(center, zoom);
    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19,
        attribution: '&copy; <a href="http://www.openstreetmap.org/copyright">OpenStreetMap</a>'
    }).addTo(map);

    map.on('click', function(e) {
        //var marker = L.marker(e.latlng).addTo(mymap)
        console.log("map click at " + e.latlng);
        // TODO? retrieve polygon etc from server
    });

    map.on('mousedown', onMapMouseDownForSelection);
}

/**
 * Toggles rectangular area selection mode. While active, map dragging is disabled
 * so a mousedown+drag on the map draws a selection rectangle instead of panning.
 */
function toggleSelectArea() {
    selectAreaMode = !selectAreaMode;
    $("#btn_selectarea").toggleClass("w3-blue", !selectAreaMode);
    $("#btn_selectarea").toggleClass("w3-green", selectAreaMode);
    if (selectAreaMode) {
        map.dragging.disable();
        showMessage("Click and drag on the map to select a rectangular area");
    } else {
        map.dragging.enable();
    }
}

function onMapMouseDownForSelection(e) {
    if (!selectAreaMode) {
        return;
    }
    selectionStart = e.latlng;
    if (selectionRectangle != null) {
        map.removeLayer(selectionRectangle);
        selectionRectangle = null;
    }
    map.on('mousemove', onMapMouseMoveForSelection);
    map.on('mouseup', onMapMouseUpForSelection);
}

function onMapMouseMoveForSelection(e) {
    var bounds = L.latLngBounds(selectionStart, e.latlng);
    if (selectionRectangle == null) {
        selectionRectangle = L.rectangle(bounds, {color: "orange", weight: 2, fillOpacity: 0.1}).addTo(map);
    } else {
        selectionRectangle.setBounds(bounds);
    }
}

function onMapMouseUpForSelection(e) {
    map.off('mousemove', onMapMouseMoveForSelection);
    map.off('mouseup', onMapMouseUpForSelection);
    if (selectionStart == null) {
        return;
    }
    map.removeLayer(selectionRectangle);
    selectionRectangle = null;
    var bounds = L.latLngBounds(selectionStart, e.latlng);
    selectionStart = null;
    toggleSelectArea();
    loadTileImage(bounds);
}

/**
 * Calls the tile image API for the selected bounding box and shows the returned
 * image as an overlay on the map, positioned exactly over the selected area.
 */
function loadTileImage(bounds) {
    var sw = bounds.getSouthWest();
    var ne = bounds.getNorthEast();
    var url = serviceshost + "/worldmesh/tile/image"
        + "?min.lat=" + sw.lat + "&min.lon=" + sw.lng
        + "&max.lat=" + ne.lat + "&max.lon=" + ne.lng;
    doGetBlob(url, blob => {
        if (selectionImageOverlay != null) {
            map.removeLayer(selectionImageOverlay);
        }
        var objectUrl = URL.createObjectURL(blob);
        //selectionImageOverlay = L.imageOverlay(objectUrl, bounds, {opacity: 0.8}).addTo(map);
        showMessage("Tile image loaded");
    });
}

/**
 *useful??
 */
function populateOptions(options, valueArray, getter, withEmptyLine) {
    console.log("populateOptions");
    var idx = 0;

}

/**
 * For select boxes
 * Also for datalists of inputs
 */
function addOption(inputid, value) {
    var element = document.getElementById(inputid);
    //console.log("element:",element);

    if (element.options != null) {
        element.options.add(new Option(value));
        return;
    }
    if (element.list != null) {
        var datalist = element.list;//document.getElementById(element.list);
        //console.log("datalist of" + element.list, datalist);
        // not possible datalist.add(value);
        var option = document.createElement('option');
        option.value = value;
        datalist.appendChild(option);
        return;
    }
}

/**
 * Also for datalists of inputs
 */
function clearOptions(inputid) {
    var element = document.getElementById(inputid);
    if (element == null) {
        console.warn("input not found:" + inputid);
    }

    if (element.options != null) {
        //not working element.options.innerHTML = "";
        //$("#droplist").empty();
        while (element.options.length > 0) {
                element.remove(0);
        }
        return;
    }
    if (element.list != null) {
        var datalist = element.list;
         /*while (datalist.children.length > 0) {
                    datalist.children[0].remove();
                }*/
        datalist.replaceChildren();
        return;
    }
}

/**
 * Returns the runway data structure
 */
function getRunwayOfAirport(airport, fromNumber) {
    for (var i = 0; i < airport.runways.length; i++) {
        if (airport.runways[i].fromNumber == fromNumber) {
            return airport.runways[i];
        }
    }
    console.warn("runway " + fromNumber + " not found at airport ", airport);
    return null;
}

/**
 * Returns the runway data structure
 */
function getRunwayFromUserSelection(idsuffix) {
    var airport = allAirports.get(getInputMeshName(idsuffix));
    if (airport == null) {
        return null;
    }
    var inputRunway = getInputRunway(idsuffix);
    if (inputRunway == null) {
        return null;
    }
    var runway = getRunwayOfAirport(airport, inputRunway);
    return runway;
}

/**
 * After each action
 */
function updateStatus() {
    // disable all buttons as a default setting
    $("#btn_createmesh").prop("disabled",true);
    $("#btn_deletemesh").prop("disabled",true);
    $("#btn_populatemesh").prop("disabled",true);

    if (currentMesh != null) {
        $("#btn_deletemesh").prop("disabled",false);
        $("#btn_populatemesh").prop("disabled",false);
    } else {
        $("#btn_createmesh").prop("disabled",false);
    }
}

/**
 * cannot be included above because of asny load of geoRoute
 */
function updateStatusForGeoRoute() {
    if (foundGeoRoute != null) {
        $("#btn_launch_route").prop("disabled",false);
        $("#btn_launch_route_vr").prop("disabled",false);
    }

}

/**
 * 'oninput' callback for mesh input field.
 */
function meshNameChanged() {
    var meshName = getInputMeshName();
    console.log("meshName changed to " + meshName);
/*
    var runwayoptions = document.getElementById("sel_runway_"+idsuffix).options;
    clearOptions("sel_runway_"+idsuffix);

    if (icao.length == 4) {*/
        loadMesh(meshName, null);
    /*} else {
        // Only start searching with 3 characters. Starting with 2 returns too much data that takes too long and doesn't
        // fit into the select box
        if (icao.length >= 3) {
            searchAirport(icao, idsuffix);
        }
    }*/
}

function createMesh() {
    var meshName = $("#inp_meshname").val();
    console.log("Creating mesh " + meshName);
    var body = {};
    doPost(serviceshost+"/worldmesh/mesh?meshName=" + meshName, json => {
        console.log("got " + json);
        showMeshFromResponse(json);
        populateOsmDatasets(meshName);
        showMessage("Mesh "+meshName+" created");
        currentMesh = meshName;
        updateStatus();
    }, body);
}

function populateMesh() {
    var meshName = $("#inp_meshname").val();
    var dataset = $("#sel_osmdataset").val();
    console.log("Populating mesh " + meshName + " with " + dataset);
    // put just the file name
    httpPut(serviceshost+"/worldmesh/mesh?meshName=" + meshName, json => {
        console.log("got " + json);
        showMeshFromResponse(json);
        showMessage("Mesh "+meshName+" populated");
    }, dataset);
}

function deleteMesh() {
    var meshName = $("#inp_meshname").val();
    console.log("Deleting mesh " + meshName);
    var body = {};
    httpDelete(serviceshost+"/worldmesh/mesh?meshName=" + meshName, json => {
        console.log("got " + json);
        currentMesh = null;
        showMessage("Mesh "+meshName+" deleted");
        updateStatus();
    }, body);
}

/**
 * No search. 'meshName' must be the full pure name.
 */
function loadMesh(meshName, selectValue) {
    console.log("Loading mesh " + meshName);
    // Initially clear everything
    clearList("ul_failures");

    doGet(serviceshost+"/worldmesh/mesh?meshName="+meshName, json => {
        console.log("got " + json);
        showMeshFromResponse(json);
        populateOsmDatasets(meshName);
        showMessage(json.polygons.length + " polygons found");
        currentMesh = meshName;
        updateStatus();
    });
}

function showMeshFromResponse(response) {
    clearList("ul_failures");
    showPolygons(response.polygons);
    if (response.failures != null) {
        response.failures.forEach(failure => {
            var secret_id = "inp_" + getUniqueId();
            var osmLink = createLink(failure.sourceRef);
            var svgLink = createFailureSvgLink(failure.id);
            var retryLink = createFailureRetryLink(failure.sourceRef);

            var content     = "<p>" + failure.message  + " " + osmLink.html + " " + svgLink + " " + retryLink + "</p>";//"<div id='" + secret_id + "' class='w3-bar w3-white'>xx</div>";
            addListItem("ul_failures", content, "w3-bar");
            console.log("Building link " + content);

            if (failure.polygon != null){
                console.log("Showing failure polygon");
                showPolygon(failure.polygon, "red");
            };
        });
    }
}

/**
 * Clickable icon that opens the failure's SVG (served by MeshController) in a new browser tab.
 * Returns an empty string when the failure has no persistence id.
 */
function createFailureSvgLink(failureId) {
    if (failureId == null) {
        return "";
    }
    var href = serviceshost + "/worldmesh/mesh/failure/" + failureId + "/svg";
    return '<a href="' + href + '" target="_blank" title="Show failure SVG">'
        + '<i class="fa fa-picture-o" aria-hidden="true"></i></a>';
}

/**
 * Clickable icon that re-runs mesh processing for the single OSM way that produced this failure.
 * The osm way id is taken from the failure's sourceRef (.../way/<id>). Uses the currently selected
 * OSM dataset, just like populateMesh().
 */
function createFailureRetryLink(sourceRef) {
    var osmWayId = StringUtils.substringAfterLast(sourceRef, "/");
    if (!osmWayId) {
        return "";
    }
    return '<a href="#" title="Retry processing this way" onclick="retryFailure(\'' + osmWayId + '\'); return false;">'
        + '<i class="fa fa-refresh" aria-hidden="true"></i></a>';
}

function retryFailure(osmWayId) {
    var meshName = $("#inp_meshname").val();
    var dataset = $("#sel_osmdataset").val();
    console.log("Retrying way " + osmWayId + " of mesh " + meshName + " with " + dataset);
    httpPut(serviceshost + "/worldmesh/mesh?meshName=" + meshName + "&osmwayid=" + osmWayId, json => {
        console.log("got " + json);
        showMeshFromResponse(json);
        showMessage("Retried way " + osmWayId);
    }, dataset);
}

function buildLatLng(e) {
    return new L.LatLng(e.lat, e.lon);
}

function buildLatLngFromString(s) {
    var parts = s.split(",");
    return new L.LatLng(parts[0], parts[1]);
}

function buildPolygon(p, color) {
    var latlngs = [];
    p.points.forEach(point => {
        latlngs.push(buildLatLng(point));
    });
    return L.polygon(latlngs, {color: color, weight: 1, fillOpacity: 0.0 });
}

function showGeoRoute(geoRoute) {
    //console.log("geoRoute="+geoRoute);
    if (map_routeMarker != null) {
        map.removeLayer(map_routeMarker);
        map_routeMarker = null;
    }
    var parts = geoRoute.split("->");
    console.log(parts);
    var lastlatLng = null;
    var latLngs = new Array();
    parts.forEach(part => {
        var subparts = part.split(":");
        var latLng = buildLatLngFromString(subparts[1]);
        if (lastlatLng != null) {
            //L.polyline([lastlatLng,latLng], {color: 'red'}).addTo(map);
            latLngs.push([lastlatLng,latLng]);
        }
        lastlatLng = latLng;
    });
    // this is no polygon, but just a line
    map_routeMarker = L.polyline(latLngs, {color: 'blue'});
    map_routeMarker.addTo(map);
}

/**
 * Returns pure icao without appended name. idsuffix is 'from' or 'to'.
 */
function getInputMeshName() {
    var v= $("#inp_meshname").val();
    // ignore optional name
    return v;//.substring(0,4);
}

function getInputRunway(idsuffix) {
    var val = $("#sel_runway_"+idsuffix).val();
    // check for blank
    if (!val) {
        return null;
    }
    return val;
}

function showPolygons(polygons) {
    clearTileGroup();
    tileGroup = L.layerGroup();
    polygons.forEach(polygon => {
        if (polygon != null) {
            showPolygon(polygon, "black");
        }
    });
    tileGroup.addTo(map);
    relocateMap(polygons[0].points[0]);
    updateStatus();
}

function showPolygon(polygon, color) {
    buildPolygon(polygon, color).addTo(tileGroup);
}

function relocateMap(pointFromResponse) {
    var latlng = buildLatLng(pointFromResponse);
    var point = L.Projection.Mercator.project(latlng);
    //console.log(latlng, point);
    // 16 fits for Desdorf
    var zoom = 16;//11;
    map.setView(latlng, zoom);
}

function clearTileGroup() {
    if (tileGroup != null) {
        tileGroup.removeFrom(map);
    }
    tileGroup = null;
}

function addWellKnownAirportsToSelectBox(idsuffix) {
    wellKnownAirports.forEach(a => addOption("inp_icao_" + idsuffix, a));
}

function showMessage(message) {
     $("#p_message").html(message);
     setTimeout(function(){$("#p_message").html("");}, 5000)
}

function showError(message) {
     $("#p_message").html(message);
     setTimeout(function(){$("#p_errormessage").html("");}, 5000)
}

function populateOsmDatasets(meshName) {
    doGet(serviceshost+"/worldmesh/osm?meshName="+meshName, json => {
        console.log("got ", json);
        clearOptions("sel_osmdataset");
        //addOption("sel_runway_"+idsuffix, " ");
        json.datasets.forEach(dataset => {
            //console.log("adding ", runway);
            addOption("sel_osmdataset", dataset);
        });

    });
   /*allAirports.set(icao, json);
        clearOptions("sel_runway_"+idsuffix);
        addOption("sel_runway_"+idsuffix, " ");
        json.runways.forEach(runway => {
            //console.log("adding ", runway);
            addOption("sel_runway_"+idsuffix, runway.fromNumber);
            console.log("runway:", runway);
            var latlng = buildLatLng(runway.from);
            var point = L.Projection.Mercator.project(latlng);
            //console.log(latlng, point);
            var zoom = 11;
            map.setView(latlng, zoom);
            var marker = L.marker([latlng.lat, latlng.lng]).addTo(map);
        });
        if (selectValue != null) {
            $("#sel_runway_"+ idsuffix).val(selectValue);
        }
        updateStatus();*/
}

/**
 * init for travelworld.html
 */
function init() {
    var url = new URL(window.location.href);
    console.log("url=" + url);
    var hostparam = url.searchParams.get("host");
    if (hostparam != null) {
        host = hostparam;
        $("#debuginfo").html("(host="+hostparam+")");
    }
    var serviceshostparam = url.searchParams.get("serviceshost");
    if (serviceshostparam != null) {
        serviceshost = serviceshostparam;
        $("#debuginfo").html("(serviceshost="+serviceshostparam+")");
    }

    var initialMeshName = "Desdorf";
    var meshnameparam = url.searchParams.get("meshname");
    if (meshnameparam != null) {
        initialMeshName = meshnameparam;
        console.log("initialMeshName="+initialMeshName);
    }

    initMap();

    // debug helper for geoRoutes
    var geoRouteparam = url.searchParams.get("geoRoute");
    if (geoRouteparam != null) {
        showGeoRoute(geoRouteparam);
    }

    // Setting a default value will reduce the datalist options displayed to only fitting values! This is 'intended by 'browser/spec'?
    document.getElementById("inp_meshname").value = initialMeshName;
    loadMesh(initialMeshName, null);

    /*clearOptions("inp_icao_from");
    addWellKnownAirportsToSelectBox("from");

    clearOptions("inp_icao_to");
    addWellKnownAirportsToSelectBox("to");
*/
    updateStatus();
}

