/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.dirt.integration.bus.kryo;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import org.junit.Test;

import org.springframework.xd.dirt.integration.bus.serializer.kryo.FileKryoRegistrar;
import org.springframework.xd.dirt.integration.bus.serializer.kryo.PojoCodec;

import static org.junit.Assert.assertEquals;


/**
 * @author David Turanski
 */
public class KryoFileCodecTests {

 @Test
 public void test() throws IOException {

  PojoCodec pc = new PojoCodec(new FileKryoRegistrar());
  File file = new File("/foo/bar");
  ByteArrayOutputStream bos = new ByteArrayOutputStream();
  pc.serialize(file, bos);
  File file2 = (File) pc.deserialize(bos.toByteArray(), File.class);
  assertEquals(file, file2);
 }
}