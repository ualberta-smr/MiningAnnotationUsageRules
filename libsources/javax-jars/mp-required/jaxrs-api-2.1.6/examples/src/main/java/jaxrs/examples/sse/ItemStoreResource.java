/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package jaxrs.examples.sse;

import java.io.IOException;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseBroadcaster;
import javax.ws.rs.sse.SseEventSink;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * A resource for storing named items.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
@SuppressWarnings("VoidMethodAnnotatedWithGET")
@Path("items")
@Singleton
public class ItemStoreResource {

    private static final Logger LOGGER = Logger.getLogger(ItemStoreResource.class.getName());

    private final ReentrantReadWriteLock storeLock = new ReentrantReadWriteLock();
    private final LinkedList<String> itemStore = new LinkedList<>();

    private final Sse sse;
    private final SseBroadcaster broadcaster;

    @Inject
    public ItemStoreResource(Sse sse) {
        this.sse = sse;
        this.broadcaster = sse.newBroadcaster();

        broadcaster.onError((subscriber, e) ->
                LOGGER.log(Level.WARNING, "An exception has been thrown while broadcasting to an event output.", e));

        broadcaster.onClose(subscriber -> LOGGER.log(Level.INFO, "SSE event output has been closed."));
    }

    private static volatile long reconnectDelay = 0;

    /**
     * List all stored items.
     *
     * @return list of all stored items.
     */
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String listItems() {
        try {
            storeLock.readLock().lock();
            return itemStore.toString();
        } finally {
            storeLock.readLock().unlock();
        }
    }

    /**
     * Receive & process commands sent by the test client that control the internal resource state.
     * <p>
     * Following is the list of recognized commands:
     * <ul>
     * <li><b>disconnect</b> - disconnect all registered event streams.</li>
     * <li><b>reconnect now</b> - enable client reconnecting.</li>
     * <li><b>reconnect &lt;seconds&gt;</b> - disable client reconnecting.
     * Reconnecting clients will receive a HTTP 503 response with
     * {@value javax.ws.rs.core.HttpHeaders#RETRY_AFTER} set to the amount of
     * milliseconds specified.</li>
     * </ul>
     *
     * @param command command to be processed.
     * @return message about processing result.
     * @throws BadRequestException in case the command is not recognized or not specified.
     */
    @POST
    @Path("commands")
    public String processCommand(String command) {
        if (command == null || command.isEmpty()) {
            throw new BadRequestException("No command specified.");
        }

        if ("disconnect".equals(command)) {
            broadcaster.close();
            return "Disconnected.";
        } else if (command.length() > "reconnect ".length() && command.startsWith("reconnect ")) {
            final String when = command.substring("reconnect ".length());
            try {
                reconnectDelay = "now".equals(when) ? 0 : Long.parseLong(when);
                return "Reconnect strategy updated: " + when;
            } catch (NumberFormatException ignore) {
                // ignored
            }
        }

        throw new BadRequestException("Command not recognized: '" + command + "'");
    }

    /**
     * Connect or re-connect to SSE event stream.
     *
     * @param lastEventId Value of custom SSE HTTP <code>{@value javax.ws.rs.core.HttpHeaders#LAST_EVENT_ID_HEADER}</code> header.
     *                    Defaults to {@code -1} if not set.
     * @param serverSink new SSE server sink stream representing the (re-)established SSE client connection.
     * @throws InternalServerErrorException in case replaying missed events to the reconnected output stream fails.
     * @throws ServiceUnavailableException  in case the reconnect delay is set to a positive value.
     */
    @GET
    @Path("events")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void itemEvents(
            @HeaderParam(HttpHeaders.LAST_EVENT_ID_HEADER) @DefaultValue("-1") int lastEventId,
            @Context SseEventSink serverSink) {

        if (lastEventId >= 0) {
            LOGGER.info("Received last event id :" + lastEventId);

            // decide the reconnect handling strategy based on current reconnect delay value.
            final long delay = reconnectDelay;
            if (delay > 0) {
                LOGGER.info("Non-zero reconnect delay [" + delay + "] - responding with HTTP 503.");
                throw new ServiceUnavailableException(delay);
            } else {
                LOGGER.info("Zero reconnect delay - reconnecting.");
                try {
                    replayMissedEvents(lastEventId, serverSink);
                } catch (IOException e) {
                    // handle I/O failure.
                }
            }
        }

        broadcaster.register(serverSink);
    }

    private void replayMissedEvents(final int lastEventId, final SseEventSink eventOutput) throws IOException {
        try {
            storeLock.readLock().lock();
            final int firstUnreceived = lastEventId + 1;
            final int missingCount = itemStore.size() - firstUnreceived;
            if (missingCount > 0) {
                LOGGER.info("Replaying events - starting with id " + firstUnreceived);
                final ListIterator<String> it = itemStore.subList(firstUnreceived, itemStore.size()).listIterator();
                while (it.hasNext()) {
                    eventOutput.send(createItemEvent(it.nextIndex() + firstUnreceived, it.next()));
                }
            } else {
                LOGGER.info("No events to replay.");
            }
        } finally {
            storeLock.readLock().unlock();
        }
    }

    /**
     * Add new item to the item store.
     * <p>
     * Invoking this method will fire 2 new SSE events - 1st about newly added item and 2nd about the new item store size.
     *
     * @param name item name.
     */
    @POST
    public void addItem(@FormParam("name") String name) {
        // Ignore if the request was sent without name parameter.
        if (name == null) {
            return;
        }

        final int eventId;
        try {
            storeLock.writeLock().lock();
            eventId = itemStore.size();
            itemStore.add(name);
            // Broadcasting an un-named event with the name of the newly added item in data
            broadcaster.broadcast(createItemEvent(eventId, name));
            // Broadcasting a named "size" event with the current size of the items collection in data
            broadcaster.broadcast(sse.newEventBuilder().name("size").data(Integer.class, eventId + 1).build());
        } finally {
            storeLock.writeLock().unlock();
        }
    }

    private OutboundSseEvent createItemEvent(final int eventId, final String name) {
        Logger.getLogger(ItemStoreResource.class.getName())
              .info("Creating event id [" + eventId + "] name [" + name + "]");
        return sse.newEventBuilder().id("" + eventId).data(String.class, name).build();
    }
}
