/*
 * Copyright 2014 Yahoo! Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the License); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an AS IS BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.yahoo.sshd.server.logging;

public enum SshRequestStatus {

    OK(200, "GET"), //
    CREATED(201, "PUT"), //
    ACCEPTED(202, "Accepted"), //
    NO_CONTENT(204, "No Content"), //
    MOVED_PERMANENTLY(301, "Moved Permanently"), //
    SEE_OTHER(303, "See Other"), //
    NOT_MODIFIED(304, "Not Modified"), //
    TEMPORARY_REDIRECT(307, "Temporary Redirect"), //
    BAD_REQUEST(400, "Bad Request"), //
    UNAUTHORIZED(401, "Unauthorized"), //
    FORBIDDEN(403, "Forbidden"), //
    NOT_FOUND(404, "Not Found"), //
    METHOD_NOT_ALLOWED(405, "Method Not Allowed"),
    NOT_ACCEPTABLE(406, "Not Acceptable"), //
    CONFLICT(409, "Conflict"), //
    GONE(410, "Gone"), //
    PRECONDITION_FAILED(412, "Precondition Failed"), //
    UNSUPPORTED_MEDIA_TYPE(415, "Unsupported Media Type"), //
    INTERNAL_SERVER_ERROR(500, "Internal Server Error"), //
    SERVICE_UNAVAILABLE(503, "Service Unavailable"); //

    private final int code;
    private final String reason;

    SshRequestStatus(final int statusCode, final String reasonPhrase) {
        this.code = statusCode;
        this.reason = reasonPhrase;
    }

    public int getStatusCode() {
        return code;
    }

    public String getReasonPhrase() {
        return toString();
    }

    @Override
    public String toString() {
        return reason;
    }

}
