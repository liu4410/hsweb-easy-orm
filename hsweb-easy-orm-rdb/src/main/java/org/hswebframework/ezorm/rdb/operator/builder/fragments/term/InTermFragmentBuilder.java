package org.hswebframework.ezorm.rdb.operator.builder.fragments.term;

import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.rdb.meta.RDBColumnMetadata;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.EmptySqlFragments;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.PrepareSqlFragments;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.SqlFragments;

import java.util.List;

public class InTermFragmentBuilder extends AbstractTermFragmentBuilder {

    private String symbol;

    public InTermFragmentBuilder(String termType, String name, boolean isNot) {
        super(termType, name);
        symbol = isNot ? "not in(" : "in(";
    }

    @Override
    public SqlFragments createFragments(String columnFullName, RDBColumnMetadata column, Term term) {

        List<Object> value = convertList(column, term);
        if (value == null || value.isEmpty()) {
            return EmptySqlFragments.INSTANCE;
        }
        int len = value.size();

        PrepareSqlFragments fragments = PrepareSqlFragments.of();
        if (len > 500) {
            fragments.addSql("(");
        }
        fragments.addSql(columnFullName)
                .addSql(symbol);

        int flag = 0;
        for (int i = 0; i < len; i++) {
            if (flag++ != 0) {
                fragments.addSql(",");
            }
            fragments.addSql("?");
            if (flag > 500 && i != len - 1) {
                flag = 0;
                fragments.addSql(") or")
                        .addSql(columnFullName)
                        .addSql(symbol);
            }
        }
        if (len > 500) {
            fragments.addSql(")");
        }
        return fragments
                .addSql(")")
                .addParameter(value);
    }
}
