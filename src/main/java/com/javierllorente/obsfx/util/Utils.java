/*
 * Copyright (C) 2025 Javier Llorente <javier@opensuse.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.javierllorente.obsfx.util;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author javier
 */
public class Utils {
    
    private enum DataUnit {
        Bytes, KB, MB, GB, TB
    }

    private Utils() {
    }
    
    public static String bytesToHumanReadableFormat(String bytes) {
        long longBytes = Long.parseLong(bytes);
        float humanReadableFormat = longBytes;
        int i = 0;        
        while (humanReadableFormat >= 1024 && i < DataUnit.values().length - 1) {
            humanReadableFormat /= 1024;
            i++;
        }
        DecimalFormat decimalFormat = new DecimalFormat("#.##");
        return decimalFormat.format(humanReadableFormat) + " " + DataUnit.values()[i];
    }
    
    public static long humanReadableFormatToBytes(String humanReadableFormat) {
        long bytes = 0;
        String[] parts = humanReadableFormat.split(" ");
        if (parts.length == 2) {
            try {
                NumberFormat numberFormat = NumberFormat.getInstance(Locale.getDefault());
                Number parsedNumber = numberFormat.parse(parts[0]);
                double value = parsedNumber.doubleValue();
                DataUnit unit = DataUnit.valueOf(parts[1]);
                switch (unit) {
                    case Bytes ->
                        bytes = (long) value;
                    case KB ->
                        bytes = (long) (value * 1024L);
                    case MB ->
                        bytes = (long) (value * 1024L * 1024L);
                    case GB ->
                        bytes = (long) (value * 1024L * 1024L * 1024L);
                    case TB ->
                        bytes = (long) (value * 1024L * 1024L * 1024L * 1024L);
                }
            } catch (ParseException ex) {
                Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return bytes;
    }
}

