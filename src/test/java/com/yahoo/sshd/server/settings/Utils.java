package com.yahoo.sshd.server.settings;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.commons.configuration.Configuration;

public class Utils {
    public static Configuration getConfigMock() {
        return new Configuration() {

            @Override
            public Configuration subset(String prefix) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public void setProperty(String key, Object value) {
                // TODO Auto-generated method stub

            }

            @Override
            public boolean isEmpty() {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public String[] getStringArray(String key) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String getString(String key, String defaultValue) {
                return defaultValue;
            }

            @Override
            public String getString(String key) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public Short getShort(String key, Short defaultValue) {
                return defaultValue;
            }

            @Override
            public short getShort(String key, short defaultValue) {
                return defaultValue;
            }

            @Override
            public short getShort(String key) {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public Object getProperty(String key) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public Properties getProperties(String key) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public Long getLong(String key, Long defaultValue) {
                return defaultValue;
            }

            @Override
            public long getLong(String key, long defaultValue) {
                return defaultValue;
            }

            @Override
            public long getLong(String key) {
                // TODO Auto-generated method stub
                return 0;
            }

            @SuppressWarnings("unchecked")
            @Override
            public List<Object> getList(String key, List<?> defaultValue) {
                return (List<Object>) defaultValue;
            }

            @Override
            public List<Object> getList(String key) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public Iterator<String> getKeys(String prefix) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public Iterator<String> getKeys() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public Integer getInteger(String key, Integer defaultValue) {
                return defaultValue;
            }

            @Override
            public int getInt(String key, int defaultValue) {
                return defaultValue;
            }

            @Override
            public int getInt(String key) {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public Float getFloat(String key, Float defaultValue) {
                return defaultValue;
            }

            @Override
            public float getFloat(String key, float defaultValue) {
                return defaultValue;
            }

            @Override
            public float getFloat(String key) {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public Double getDouble(String key, Double defaultValue) {
                return defaultValue;
            }

            @Override
            public double getDouble(String key, double defaultValue) {
                return defaultValue;
            }

            @Override
            public double getDouble(String key) {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public Byte getByte(String key, Byte defaultValue) {
                return defaultValue;
            }

            @Override
            public byte getByte(String key, byte defaultValue) {
                return defaultValue;
            }

            @Override
            public byte getByte(String key) {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public Boolean getBoolean(String key, Boolean defaultValue) {
                return defaultValue;
            }

            @Override
            public boolean getBoolean(String key, boolean defaultValue) {
                return defaultValue;
            }

            @Override
            public boolean getBoolean(String key) {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public BigInteger getBigInteger(String key, BigInteger defaultValue) {
                return defaultValue;
            }

            @Override
            public BigInteger getBigInteger(String key) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public BigDecimal getBigDecimal(String key, BigDecimal defaultValue) {
                return defaultValue;
            }

            @Override
            public BigDecimal getBigDecimal(String key) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public boolean containsKey(String key) {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public void clearProperty(String key) {
                // TODO Auto-generated method stub

            }

            @Override
            public void clear() {
                // TODO Auto-generated method stub

            }

            @Override
            public void addProperty(String key, Object value) {
                // TODO Auto-generated method stub
            }
        };
    }
}
