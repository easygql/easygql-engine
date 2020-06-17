package com.easygql.util;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author guofen
 * @date 2019/11/23 12:01
 */
@Data
public class ObjectTypeInfo {
  private String id;
  private String name;
  private String alias;
  private List<ScalarFieldInfo> scalarFields = new ArrayList<>();
  private List<EnumField> enumFields = new ArrayList();
  private List<String> unreadableRoles = new ArrayList<>();
  private List<String> uninsertableRoles = new ArrayList<>();
  private List<String> undeletableRoles = new ArrayList<>();
  private List<String> unupdatableRoles = new ArrayList<>();
  private Object readConstraints;
  private Object updateConstraints;
  private Object deleteConstraints;
  private List<UniqueConstraint> uniqueConstraints = new ArrayList<>();
  private String description;
}
