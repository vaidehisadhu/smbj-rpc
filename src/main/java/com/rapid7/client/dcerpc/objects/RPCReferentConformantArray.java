/**
 * Copyright 2017, Rapid7, Inc.
 *
 * License: BSD-3-clause
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 */
package com.rapid7.client.dcerpc.objects;

import java.io.IOException;
import java.util.ArrayList;
import com.rapid7.client.dcerpc.io.PacketInput;
import com.rapid7.client.dcerpc.io.PacketOutput;
import com.rapid7.client.dcerpc.io.ndr.Marshallable;
import com.rapid7.client.dcerpc.io.ndr.Unmarshallable;

public abstract class RPCReferentConformantArray<T extends Unmarshallable & Marshallable>
        extends RPCConformantArray<T> {

    @Override
    public void unmarshalEntity(PacketInput in) throws IOException {
        if (getMaxCount() >= 0) {
            array = new ArrayList<>();
        }
        for (int i = 0; i < getMaxCount(); i++) {
            int refId = in.readReferentID();
            // Not contained in the deferrals.
            if (refId == 0) {
                array.add(null);
            } else {
                array.add(createEntity());
            }
        }
    }

    @Override
    public void unmarshalDeferrals(PacketInput in) throws IOException {
        for (T t : array) {
            if (t != null)
                t.unmarshalPreamble(in);
        }
        for (T t : array) {
            if (t != null)
                t.unmarshalEntity(in);
        }
        for (T t : array) {
            if (t != null)
                t.unmarshalDeferrals(in);
        }
    }

    @Override
    public void marshalEntity(PacketOutput out) throws IOException {
        for (T t : array) {
            if (t != null)
                out.writeReferentID();
            else
                out.writeInt(0);
        }
    }

    @Override
    public void marshalDeferrals(PacketOutput out) throws IOException {
        for (T t : array) {
            if (t != null)
                t.marshalPreamble(out);
        }
        for (T t : array) {
            if (t != null)
                t.marshalEntity(out);
        }
        for (T t : array) {
            if (t != null)
                t.marshalDeferrals(out);
        }
    }

    protected abstract T createEntity();
}
