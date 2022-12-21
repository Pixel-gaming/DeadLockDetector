package com.c0d3m4513r.deadlockdetector.api.panels.craftycontroller;

import lombok.Value;

@Value
public class Request<T>
{
    String status;
    T data;
}
