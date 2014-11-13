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
package com.yahoo.sshd.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ThreadSafeSimpleDateFormat {
    private final String format;
    private final ThreadLocal<SimpleDateFormat> tls = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat(format);
        }
    };

    public ThreadSafeSimpleDateFormat(final String format) {
        this.format = format;
    }

    public Date format(String date) throws ParseException {
        return tls.get().parse(date);
    }

    public long formatLong(String date) throws ParseException {
        return tls.get().parse(date).getTime();
    }

    public long formatLongNoException(String date) {
        if (null == date) {
            return 0L;
        }

        date = date.trim();

        if (date.isEmpty()) {
            return 0L;
        }

        try {
            return tls.get().parse(date).getTime();
        } catch (ParseException e) {
            return 0;
        }
    }

    public String formatLongToString(long startTimestamp) {
        return tls.get().format(new Date(startTimestamp));
    }
}
