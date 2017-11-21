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
import java.util.Collections;
import java.util.List;
import com.rapid7.client.dcerpc.io.PacketInput;
import com.rapid7.client.dcerpc.io.PacketOutput;
import com.rapid7.client.dcerpc.io.ndr.Marshallable;
import com.rapid7.client.dcerpc.io.ndr.Unmarshallable;

public abstract class RPCConformantArray<T> implements Unmarshallable, Marshallable {

    private int maxCount;
    protected List<T> array;

    public void setMaxCount(int maxCount) {
        this.maxCount = maxCount;
    }

    public void setArray(List<T> array) {
        this.array = array;
    }

    public int getMaxCount() {
        return maxCount;
    }

    public List<T> getArray() {
        return Collections.unmodifiableList(array);
    }

    @Override
    public void unmarshalPreamble(PacketInput in) throws IOException {
        maxCount = in.readInt();
    }

    @Override
    public abstract void unmarshalEntity(PacketInput in) throws IOException;

    @Override
    public abstract void unmarshalDeferrals(PacketInput in) throws IOException;

    @Override
    public void marshalPreamble(PacketOutput out) throws IOException {
        out.writeInt(maxCount);
    }

    @Override
    public abstract void marshalEntity(PacketOutput out) throws IOException;

    @Override
    public abstract void marshalDeferrals(PacketOutput out) throws IOException;
}
