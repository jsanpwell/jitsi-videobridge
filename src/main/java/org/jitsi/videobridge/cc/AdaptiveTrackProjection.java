/*
 * Copyright @ 2018 Atlassian Pty Ltd
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
package org.jitsi.videobridge.cc;

import org.jitsi.impl.neomedia.rtp.*;
import org.jitsi.service.neomedia.*;

import java.lang.ref.*;

/**
 * Filters the packets coming from a specific {@link MediaStreamTrackDesc}
 * based on the currently forwarded subjective quality index. It's also taking
 * care of upscaling and downscaling. It is also responsible for rewriting the
 * forwarded packets so that the the quality switches are transparent from the
 * receiver. See svc.md in the doc folder for more details.
 *
 * @author George Politis
 */
public abstract class AdaptiveTrackProjection
{
    /**
     * An empty {@link RawPacket} array that is used as a return value and
     * indicates that the input packet needs to be dropped.
     */
    public static final RawPacket[] DROP_PACKET_ARR = new RawPacket[0];

    /**
     * An empty {@link RawPacket} array that is used as a return value when no
     * packets need to be piggy-backed.
     */
    public static final RawPacket[] EMPTY_PACKET_ARR = new RawPacket[0];

    /**
     * A {@link WeakReference} to the {@link MediaStreamTrackDesc} that owns
     * the packets that this instance filters. We keep a {@link WeakReference}
     * instead of a reference to allow the channel/stream/etc objects to be
     * de-allocated in case the sending participant leaves the conference.
     */
    private final WeakReference<MediaStreamTrackDesc> weakSource;

    /**
     * The ideal quality index for this track projection.
     */
    private int idealIndex = RTPEncodingDesc.SUSPENDED_INDEX;

    /**
     * Ctor.
     *
     * @param source the {@link MediaStreamTrackDesc} that owns the packets
     * that this instance filters. If the source is null, no keyframes will be
     * requested and the outgoing SSRC will be "1".
     */
    public AdaptiveTrackProjection(MediaStreamTrackDesc source)
    {
        weakSource = source == null ? null : new WeakReference<>(source);
    }

    /**
     * @return the {@link MediaStreamTrackDesc} that owns the packets that this
     * instance filters. Note that this may return null.
     */
    public MediaStreamTrackDesc getSource()
    {
        return weakSource == null ? null : weakSource.get();
    }

    /**
     * @return the ideal quality for this track projection.
     */
    int getIdealIndex()
    {
        return idealIndex;
    }

    /**
     * Update the ideal quality for this track projection.
     *
     * @param value the ideal quality for this track projection.
     */
    void setIdealIndex(int value)
    {
        idealIndex = value;
    }

    /**
     * Gets the target index value for this track projection.
     *
     * @return the target index value for this track projection.
     */
    public abstract int getTargetIndex();

    /**
     * Sets the target index value for this track projection.
     *
     * @param value the new target index value for this track projection.
     */
    public abstract void setTargetIndex(int value);

    /**
     * Determines whether an RTP packet needs to be accepted or not.
     *
     * @param rtpPacket the RTP packet to determine whether to accept or not.
     * @return true if the packet is accepted, false otherwise.
     */
    public abstract boolean accept(RawPacket rtpPacket);

    /**
     * Rewrites both RTP and RTCP packets and returns any additional packets
     * that need to be piggy-backed.
     *
     * @param rtpRtcpPacket an RTP or an RTCP packet to rewrite.
     * @return any piggy-backed packets to include with the packet, or
     * {@link #DROP_PACKET_ARR} if the packet needs to be dropped.
     */
    public abstract RawPacket[] rewrite(RawPacket rtpRtcpPacket);

    /**
     * @return the SSRC of the track projection.
     */
    public abstract long getSSRC();
}