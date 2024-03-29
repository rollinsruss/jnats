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
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public abstract class AbstractClientChannelHandler extends SimpleChannelHandler {

	// Access must be synchronized on self.
	private final Queue<ClientPingMessage> pingQueue = new LinkedList<ClientPingMessage>();

	// Access must be synchronized on self.
	private final Queue<ClientRequest> requestQueue = new LinkedList<ClientRequest>();

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
		if (e.getMessage() instanceof ServerMessage) {
			if (e.getMessage() instanceof ServerPublishMessage) {
				publishedMessage(ctx, (ServerPublishMessage) e.getMessage());
			} else if (e.getMessage() instanceof ServerPingMessage) {
				serverPing(ctx);
			} else if (e.getMessage() instanceof  ServerOkMessage) {
				okResponse(ctx, pollRequestQueue(), (ServerOkMessage) e.getMessage());
			} else if (e.getMessage() instanceof ServerErrorMessage) {
				errorResponse(ctx, pollRequestQueue(), (ServerErrorMessage) e.getMessage());
			} else if (e.getMessage() instanceof ServerPongMessage) {
				pongResponse(ctx, pollPingQueue(), (ServerPongMessage) e.getMessage());
			} else if (e.getMessage() instanceof ServerInfoMessage) {
				serverInfo(ctx, (ServerInfoMessage) e.getMessage());
			} else {
				throw new Error("Received a server response of an unknown type: " + e.getMessage().getClass().getName());
			}
		} else {
			super.messageReceived(ctx, e);
		}
	}

	@Override
	public void writeRequested(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
		final Object message = e.getMessage();
		if (message instanceof ClientPingMessage) {
			synchronized (pingQueue) {
				pingQueue.add((ClientPingMessage) message);
			}
		} else if (message instanceof ClientRequest) {
			synchronized (requestQueue) {
				requestQueue.add((ClientRequest) message);
			}
		}
		super.writeRequested(ctx, e);
	}

	@Override
	public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		synchronized (pingQueue) {
			handleOrphanedPings(ctx, pingQueue);
			pingQueue.clear();
		}
		synchronized (requestQueue) {
			handleOrphanedRequests(ctx, requestQueue);
			requestQueue.clear();
		}
	}

	protected abstract void handleOrphanedPings(ChannelHandlerContext ctx, Collection<ClientPingMessage> pings);

	protected abstract void handleOrphanedRequests(ChannelHandlerContext ctx, Collection<ClientRequest> requests);

	protected abstract void publishedMessage(ChannelHandlerContext ctx, ServerPublishMessage message);

	protected void serverPing(ChannelHandlerContext ctx) {
		ctx.getChannel().write(ClientPongMessage.PONG);
	}

	protected abstract void pongResponse(ChannelHandlerContext ctx, ClientPingMessage pingMessage, ServerPongMessage pongMessage);

	protected abstract void serverInfo(ChannelHandlerContext ctx, ServerInfoMessage infoMessage);

	protected abstract void okResponse(ChannelHandlerContext ctx, ClientRequest request, ServerOkMessage okMessage);

	protected abstract void errorResponse(ChannelHandlerContext ctx, ClientRequest request, ServerErrorMessage errorMessage);

	private ClientRequest pollRequestQueue() {
		final ClientRequest request;
		synchronized (requestQueue) {
			request = requestQueue.poll();
		}
		if (request == null) {
			throw new NatsException("Received a response from the NATS server for an unknown request.");
		}
		return request;
	}

	private ClientPingMessage pollPingQueue() {
		final ClientPingMessage ping;
		synchronized (pingQueue) {
			ping = pingQueue.poll();
		}
		if (ping == null) {
			throw new NatsException("Received a pong from the NATS server for an unknown ping.");
		}
		return ping;
	}

}
