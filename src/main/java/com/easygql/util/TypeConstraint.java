package com.easygql.util;

import lombok.Data;

import java.util.HashMap;
import java.util.HashSet;
/**
 * @author guofen
 * @date 2019-10-27 16:41
 */
@Data
public class TypeConstraint {
    private HashMap rowconstraint;
    private HashSet<String> fieldset;
}
