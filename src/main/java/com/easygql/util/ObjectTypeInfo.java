package com.easygql.util;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author guofen
 * @date 2019/11/23 12:01
 */
@Data
public class ObjectTypeInfo {
  private String id;
  private String name;
  private List<ScalarFieldInfo> scalarfields = new ArrayList<>();
  private List<EnumField> enumfields = new ArrayList();
  private List<String> unreadable_roles = new ArrayList<>();
  private List<String> uninsertable_roles = new ArrayList<>();
  private List<String> undeletable_roles = new ArrayList<>();
  private List<String> unupdatable_roles = new ArrayList<>();
  private Object read_constraints;
  private Object update_constraints;
  private Object delete_constraints;
  private List<UniqueConstraint> unique_constraints = new ArrayList<>();
  private String description;
}
