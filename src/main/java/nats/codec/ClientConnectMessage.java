/*
 *   Copyright (c) 2012 Mike Heath.  All rights reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package nats.codec;

import nats.NatsException;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class ClientConnectMessage implements ClientMessage, ClientRequest {

	public static final String CMD_CONNECT = "CONNECT";

	private final ConnectBody body;

	public ClientConnectMessage(ConnectBody body) {
		this.body = body;
	}

	public ConnectBody getBody() {
		return body;
	}

	@Override
	public String encode() {
		StringBuilder builder = new StringBuilder();
		builder.append(CMD_CONNECT).append(" ");
		ObjectMapper mapper = new ObjectMapper();
		try {
			builder.append(mapper.writeValueAsString(body));
		} catch (IOException e) {
			throw new NatsException(e);
		}
		builder.append("\r\n");
		return builder.toString();
	}

}
