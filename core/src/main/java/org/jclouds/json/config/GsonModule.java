/**
 * Licensed to jclouds, Inc. (jclouds) under one or more
 * contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  jclouds licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jclouds.json.config;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.jclouds.crypto.CryptoStreams;
import org.jclouds.date.DateService;
import org.jclouds.domain.JsonBall;
import org.jclouds.json.Json;
import org.jclouds.json.internal.EnumTypeAdapterThatReturnsFromValue;
import org.jclouds.json.internal.GsonWrapper;
import org.jclouds.json.internal.NullHackJsonLiteralAdapter;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.primitives.Bytes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.internal.JsonReaderInternalAccess;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.google.inject.AbstractModule;
import com.google.inject.ImplementedBy;
import com.google.inject.Provides;

/**
 * Contains logic for parsing objects from Strings.
 * 
 * @author Adrian Cole
 */
public class GsonModule extends AbstractModule {

   @SuppressWarnings("rawtypes")
   @Provides
   @Singleton
   Gson provideGson(TypeAdapter<JsonBall> jsonAdapter, DateAdapter adapter, ByteListAdapter byteListAdapter,
            ByteArrayAdapter byteArrayAdapter, PropertiesAdapter propertiesAdapter, JsonAdapterBindings bindings)
            throws ClassNotFoundException, Exception {
      GsonBuilder builder = new GsonBuilder();

      // simple (type adapters)
      builder.registerTypeAdapter(Properties.class, propertiesAdapter.nullSafe());
      builder.registerTypeAdapter(Date.class, adapter.nullSafe());
      builder.registerTypeAdapter(new TypeToken<List<Byte>>() {
      }.getType(), byteListAdapter.nullSafe());
      builder.registerTypeAdapter(byte[].class, byteArrayAdapter.nullSafe());
      builder.registerTypeAdapter(JsonBall.class, jsonAdapter.nullSafe());

      // complicated (serializers/deserializers as they need context to operate)
      builder.registerTypeHierarchyAdapter(Enum.class, new EnumTypeAdapterThatReturnsFromValue());

      for (Map.Entry<Type, Object> binding : bindings.getBindings().entrySet()) {
         builder.registerTypeAdapter(binding.getKey(), binding.getValue());
      }

      return builder.create();
   }

   @ImplementedBy(CDateAdapter.class)
   public static abstract class DateAdapter extends TypeAdapter<Date> {

   }

   @Provides
   @Singleton
   protected TypeAdapter<JsonBall> provideJsonBallAdapter(NullHackJsonBallAdapter in) {
      return in;
   }

   public static class NullHackJsonBallAdapter extends NullHackJsonLiteralAdapter<JsonBall> {

      @Override
      protected JsonBall createJsonLiteralFromRawJson(String json) {
         return new JsonBall(json);
      }

   }

   @ImplementedBy(HexByteListAdapter.class)
   public static abstract class ByteListAdapter extends TypeAdapter<List<Byte>> {

   }

   @ImplementedBy(HexByteArrayAdapter.class)
   public static abstract class ByteArrayAdapter extends TypeAdapter<byte[]> {

   }

   @Singleton
   public static class HexByteListAdapter extends ByteListAdapter {

      @Override
      public void write(JsonWriter writer, List<Byte> value) throws IOException {
         writer.value(CryptoStreams.hex(Bytes.toArray(value)));
      }

      @Override
      public List<Byte> read(JsonReader reader) throws IOException {
         return Bytes.asList(CryptoStreams.hex(reader.nextString()));
      }

   }

   @Singleton
   public static class HexByteArrayAdapter extends ByteArrayAdapter {

      @Override
      public void write(JsonWriter writer, byte[] value) throws IOException {
         writer.value(CryptoStreams.hex(value));
      }

      @Override
      public byte[] read(JsonReader reader) throws IOException {
         return CryptoStreams.hex(reader.nextString());
      }
   }

   @Singleton
   public static class Iso8601DateAdapter extends DateAdapter {
      private final DateService dateService;

      @Inject
      public Iso8601DateAdapter(DateService dateService) {
         this.dateService = dateService;
      }

      public void write(JsonWriter writer, Date value) throws IOException {
         writer.value(dateService.iso8601DateFormat(value));
      }

      public Date read(JsonReader reader) throws IOException {
         return parseDate(reader.nextString());
      }

      protected Date parseDate(String toParse) {
         try {
            return dateService.iso8601DateParse(toParse);
         } catch (RuntimeException e) {
            return dateService.iso8601SecondsDateParse(toParse);
         }
      }

   }

   @Singleton
   public static class PropertiesAdapter extends TypeAdapter<Properties> {
      private final Provider<Gson> gson;
      private final TypeToken<Map<String, String>> mapType = new TypeToken<Map<String, String>>() {
      };

      @Inject
      public PropertiesAdapter(Provider<Gson> gson) {
         this.gson = gson;
      }

      @Override
      public void write(JsonWriter out, Properties value) throws IOException {
         Builder<String, String> srcMap = ImmutableMap.<String, String> builder();
         for (Enumeration<?> propNames = value.propertyNames(); propNames.hasMoreElements();) {
            String propName = (String) propNames.nextElement();
            srcMap.put(propName, value.getProperty(propName));
         }
         gson.get().getAdapter(mapType).write(out, srcMap.build());
      }

      @Override
      public Properties read(JsonReader in) throws IOException {
         Properties props = new Properties();
         in.beginObject();
         while (in.hasNext()) {
            JsonReaderInternalAccess.INSTANCE.promoteNameToValue(in);
            props.setProperty(in.nextString(), in.nextString());
         }
         in.endObject();
         return props;
      }

   }

   @Singleton
   public static class CDateAdapter extends DateAdapter {
      private final DateService dateService;

      @Inject
      public CDateAdapter(DateService dateService) {
         this.dateService = dateService;
      }

      public void write(JsonWriter writer, Date value) throws IOException {
         writer.value(dateService.cDateFormat(value));
      }

      public Date read(JsonReader reader) throws IOException {
         return dateService.cDateParse(reader.nextString());
      }

   }

   @Singleton
   public static class LongDateAdapter extends DateAdapter {

      public void write(JsonWriter writer, Date value) throws IOException {
         writer.value(value.getTime());
      }

      public Date read(JsonReader reader) throws IOException {
         long toParse = reader.nextLong();
         if (toParse == -1)
            return null;
         return new Date(toParse);
      }
   }

   @Singleton
   public static class JsonAdapterBindings {
      private final Map<Type, Object> bindings = Maps.newHashMap();

      @com.google.inject.Inject(optional = true)
      public void setBindings(Map<Type, Object> bindings) {
         this.bindings.putAll(bindings);
      }

      public Map<Type, Object> getBindings() {
         return bindings;
      }
   }

   @Override
   protected void configure() {
      bind(Json.class).to(GsonWrapper.class);
   }
}
