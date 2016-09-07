/*
 * Copyright (c) 2016 Cisco and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fd.honeycomb.tutorial.write;

import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.v3po.util.NamingContext;
import io.fd.honeycomb.translate.v3po.util.TranslateUtils;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.sample.plugin.rev160918.sample.plugin.params.vxlans.VxlanTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.sample.plugin.rev160918.sample.plugin.params.vxlans.VxlanTunnelKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.VppBaseCallException;
import org.openvpp.jvpp.core.dto.VxlanAddDelTunnel;
import org.openvpp.jvpp.core.dto.VxlanAddDelTunnelReply;
import org.openvpp.jvpp.core.future.FutureJVppCore;

/**
 * Writer for {@link VxlanTunnel} list node from our YANG model.
 */
public final class VxlanWriteCustomizer implements ListWriterCustomizer<VxlanTunnel, VxlanTunnelKey> {

    /**
     * JVpp APIs
     */
    private final FutureJVppCore jvppCore;
    /**
     * Shared vxlan tunnel naming context
     */
    private final NamingContext vxlanTunnelNamingContext;

    public VxlanWriteCustomizer(final FutureJVppCore jvppCore, final NamingContext vxlanTunnelNamingContext) {
        this.jvppCore = jvppCore;
        this.vxlanTunnelNamingContext = vxlanTunnelNamingContext;
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<VxlanTunnel> id,
                                       @Nonnull final VxlanTunnel dataAfter,
                                       @Nonnull final WriteContext writeContext) throws WriteFailedException {
        // Create and set vxlan tunnel add request
        final VxlanAddDelTunnel vxlanAddDelTunnel = new VxlanAddDelTunnel();
        // 1 for add, 0 for delete
        vxlanAddDelTunnel.isAdd = 1;
        // dataAfter is the new vxlanTunnel configuration
        final boolean isIpv6 = dataAfter.getSrc().getIpv6Address() != null;
        vxlanAddDelTunnel.isIpv6 = TranslateUtils.booleanToByte(isIpv6);
        vxlanAddDelTunnel.srcAddress = TranslateUtils.ipAddressToArray(isIpv6, dataAfter.getSrc());
        vxlanAddDelTunnel.dstAddress = TranslateUtils.ipAddressToArray(isIpv6, dataAfter.getDst());
        // There are other input parameters that are not exposed by our YANG model, default values will be used

        try {
            final VxlanAddDelTunnelReply replyForWrite = TranslateUtils
                    .getReplyForWrite(jvppCore.vxlanAddDelTunnel(vxlanAddDelTunnel).toCompletableFuture(), id);

            // VPP returns the index of new vxlan tunnel
            final int newVxlanTunnelIndex = replyForWrite.swIfIndex;
            // It's important to store it in context so that reader knows to which name a vxlan tunnel is mapped
            vxlanTunnelNamingContext.addName(newVxlanTunnelIndex, dataAfter.getId(), writeContext.getMappingContext());
        } catch (VppBaseCallException e) {
            throw new WriteFailedException.CreateFailedException(id, dataAfter, e);
        }
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<VxlanTunnel> id,
                                        @Nonnull final VxlanTunnel dataBefore,
                                        @Nonnull final VxlanTunnel dataAfter, @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        // Not supported at VPP API level, throw exception
        throw new WriteFailedException.UpdateFailedException(id, dataBefore, dataAfter,
                new UnsupportedOperationException("Vxlan tunnel update is not supported by VPP"));
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<VxlanTunnel> id,
                                        @Nonnull final VxlanTunnel dataBefore,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        // Create and set vxlan tunnel add request
        final VxlanAddDelTunnel vxlanAddDelTunnel = new VxlanAddDelTunnel();
        // 1 for add, 0 for delete
        vxlanAddDelTunnel.isAdd = 0;
        // Vxlan tunnel is identified by its attributes when deleting, not index, so set all attributes
        // dataBefore is the vxlan tunnel that's being deleted
        final boolean isIpv6 = dataBefore.getSrc().getIpv6Address() != null;
        vxlanAddDelTunnel.isIpv6 = TranslateUtils.booleanToByte(isIpv6);
        vxlanAddDelTunnel.srcAddress = TranslateUtils.ipAddressToArray(isIpv6, dataBefore.getSrc());
        vxlanAddDelTunnel.dstAddress = TranslateUtils.ipAddressToArray(isIpv6, dataBefore.getDst());
        // There are other input parameters that are not exposed by our YANG model, default values will be used

        try {
            final VxlanAddDelTunnelReply replyForWrite = TranslateUtils
                    .getReplyForWrite(jvppCore.vxlanAddDelTunnel(vxlanAddDelTunnel).toCompletableFuture(), id);
            // It's important to remove the mapping from context
            vxlanTunnelNamingContext.removeName(dataBefore.getId(), writeContext.getMappingContext());
        } catch (VppBaseCallException e) {
            throw new WriteFailedException.DeleteFailedException(id, e);
        }
    }
}
