//
// Copyright (c) ZeroC, Inc. All rights reserved.
//

#pragma once
["java:package:com.anmvg.celi"]
module Demo
{

interface Hello
{
    idempotent void sayHello(int delay);
    void shutdown();
}

}
