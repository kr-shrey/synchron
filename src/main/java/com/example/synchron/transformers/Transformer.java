package com.example.synchron.transformers;

import com.example.synchron.schemas.ExternalSystemSchema;
import com.example.synchron.schemas.InternalSystemSchema;
import java.util.Map;

public interface Transformer {

  String externalSystemName();

  ExternalSystemSchema toExternalSystemSchema(InternalSystemSchema record);

  Map<String, Object> toWireFormat(InternalSystemSchema record);

  InternalSystemSchema fromExternalSystemSchema(ExternalSystemSchema payload);
}
