package com.example.moemi.transit.tago;

import static com.example.moemi.transit.tago.TagoResponseParser.decimal;
import static com.example.moemi.transit.tago.TagoResponseParser.integer;
import static com.example.moemi.transit.tago.TagoResponseParser.text;

import com.example.moemi.transit.Arrival;
import com.example.moemi.transit.BusLocation;
import com.example.moemi.transit.City;
import com.example.moemi.transit.Route;
import com.example.moemi.transit.RouteStop;
import com.example.moemi.transit.Stop;

import tools.jackson.databind.JsonNode;

/**
 * Turns TAGO items into domain records. TAGO field names are all lower case; numbers may come as
 * JSON numbers or strings depending on the city, so every field is read as text first.
 */
final class TagoMapper {

    private TagoMapper() {
    }

    static City city(JsonNode item) {
        return new City(text(item, "citycode"), text(item, "cityname"));
    }

    static Stop stop(String cityCode, JsonNode item) {
        return new Stop(cityCode, text(item, "nodeid"), text(item, "nodenm"), text(item, "nodeno"),
                decimal(item, "gpslati"), decimal(item, "gpslong"));
    }

    /** A coordinate search result, which tells its own city. */
    static Stop nearbyStop(JsonNode item) {
        return stop(text(item, "citycode"), item);
    }

    static Route route(String cityCode, JsonNode item) {
        return new Route(cityCode, text(item, "routeid"), text(item, "routeno"), text(item, "routetp"),
                text(item, "startnodenm"), text(item, "endnodenm"),
                time(text(item, "startvehicletime")), time(text(item, "endvehicletime")),
                integer(item, "intervaltime"));
    }

    static RouteStop routeStop(JsonNode item) {
        Integer order = integer(item, "nodeord");
        return new RouteStop(text(item, "nodeid"), text(item, "nodenm"), text(item, "nodeno"),
                order == null ? 0 : order, text(item, "updowncd"), decimal(item, "gpslati"), decimal(item, "gpslong"));
    }

    static Arrival arrival(JsonNode item) {
        return new Arrival(text(item, "routeid"), text(item, "routeno"), text(item, "routetp"), text(item, "vehicletp"),
                integer(item, "arrprevstationcnt"), integer(item, "arrtime"));
    }

    static BusLocation busLocation(JsonNode item) {
        return new BusLocation(text(item, "vehicleno"), text(item, "nodeid"), text(item, "nodenm"),
                integer(item, "nodeord"), decimal(item, "gpslati"), decimal(item, "gpslong"));
    }

    /** {@code "0530"} or {@code 530} to {@code "05:30"}; anything else is returned as is. */
    static String time(String raw) {
        if (raw == null || !raw.matches("\\d{3,4}")) {
            return raw;
        }
        String padded = raw.length() == 3 ? "0" + raw : raw;
        return padded.substring(0, 2) + ":" + padded.substring(2);
    }
}
