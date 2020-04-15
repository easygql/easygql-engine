package com.easygql.thirdapis;

import com.easygql.util.SchemaData;
import com.easygql.util.ThirdAPIField;
import com.easygql.util.ThirdAPIInput;
import lombok.Data;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

@Data
public abstract class ThirdAPI {
  public abstract Object doWork(ThirdAPIInput thirdAPIInput);

  public abstract HashMap<String, ThirdAPIField> inputFields();

  public abstract HashMap<String, ThirdAPIField> outputFields();
}
