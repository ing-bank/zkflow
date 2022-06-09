package com.ing.zkflow.best_effort_type_resolver_test.two

import com.ing.zkflow.annotations.ASCII
import com.ing.zkflow.annotations.ZKP

@ZKP
data class TestMe(val string: @ASCII(5) String = "aa")
