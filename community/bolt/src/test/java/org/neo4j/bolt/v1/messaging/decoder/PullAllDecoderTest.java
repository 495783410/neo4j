/*
 * Copyright (c) 2002-2018 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.bolt.v1.messaging.decoder;

import org.junit.jupiter.api.Test;

import org.neo4j.bolt.logging.BoltMessageLogger;
import org.neo4j.bolt.messaging.Neo4jPack.Unpacker;
import org.neo4j.bolt.messaging.RequestMessageDecoder;
import org.neo4j.bolt.runtime.BoltResponseHandler;
import org.neo4j.bolt.v1.messaging.message.PullAll;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class PullAllDecoderTest
{
    private final BoltResponseHandler responseHandler = mock( BoltResponseHandler.class );
    private final BoltMessageLogger messageLogger = mock( BoltMessageLogger.class );
    private final RequestMessageDecoder decoder = new PullAllDecoder( responseHandler, messageLogger );

    @Test
    void shouldReturnCorrectSignature()
    {
        assertEquals( PullAll.SIGNATURE, decoder.signature() );
    }

    @Test
    void shouldReturnConnectResponseHandler()
    {
        assertEquals( responseHandler, decoder.resonseHandler() );
    }

    @Test
    void shouldDecodeAckFailure() throws Exception
    {
        assertEquals( PullAll.INSTANCE, decoder.decode( mock( Unpacker.class ) ) );
    }
}