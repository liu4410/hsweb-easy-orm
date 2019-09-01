package org.hswebframework.ezorm.rdb.meta;

import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.core.meta.Feature;
import org.hswebframework.ezorm.rdb.dialect.Dialect;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.JoinFragmentBuilder;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.QueryTermsFragmentBuilder;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.SelectColumnFragmentBuilder;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Optional.*;

@Getter
@Setter
public abstract class AbstractTableOrViewMetadata implements TableOrViewMetadata {

    private String name;

    private String alias;

    private DefaultRDBSchemaMetadata schema;

    private final Map<String, RDBColumnMetadata> allColumns = new HashMap<>();

    private List<ForeignKeyMetadata> foreignKey = new ArrayList<>();

    private Map<String, Feature> features = new HashMap<>();

    public AbstractTableOrViewMetadata() {
        //注册默认的where条件构造器
        registerFeature(QueryTermsFragmentBuilder.of(this));
        //注册默认的查询列构造器
        registerFeature(SelectColumnFragmentBuilder.of(this));
        //JOIN
        registerFeature(JoinFragmentBuilder.of(this));
    }

    public boolean isTable() {
        return this instanceof RDBTableMetadata;
    }

    public boolean isView() {
        return this instanceof RDBViewMetadata;
    }

    public void removeColumn(String name) {
        synchronized (allColumns) {
            allColumns.remove(name);
        }
    }

    @Override
    public DefaultRDBSchemaMetadata getSchema() {
        return schema;
    }

    public void addColumn(RDBColumnMetadata column) {
        synchronized (allColumns) {
            column.setOwner(this);
            allColumns.put(column.getName(), column);
            allColumns.put(column.getAlias(), column);
        }
    }

    @Override
    public List<RDBColumnMetadata> getColumns() {
        return new ArrayList<>(allColumns.values()
                .stream()
                .collect(Collectors.toMap(RDBColumnMetadata::getName, Function.identity(), (_1, _2) -> _1))
                .values());
    }

    @Override
    public List<RDBColumnMetadata> findColumns() {
        return allColumns
                .values()
                .stream()
                .flatMap(c -> getForeignKey()
                        .stream()
                        .map(ForeignKeyMetadata::getTarget)
                        .map(TableOrViewMetadata::getColumns)
                        .flatMap(Collection::stream))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<RDBColumnMetadata> getColumn(String name) {
        return ofNullable(name)
                .map(allColumns::get);
    }

    @Override
    public Optional<RDBColumnMetadata> findColumn(String name) {
        return ofNullable(name)
                .map(this::getColumn)
                .filter(Optional::isPresent)
                .orElseGet(() -> findNestColumn(name));
    }

    private Optional<RDBColumnMetadata> findNestColumn(String name) {
        if (name == null) {
            return empty();
        }

        if (name.contains(".")) {
            String[] arr = name.split("[.]");
            if (arr.length == 2) {  //table.name
                return findColumnFromSchema(schema, arr[0], arr[1]);

            } else if (arr.length == 3) { //schema.table.name
                return schema.getDatabase()
                        .getSchema(arr[0])
                        .flatMap(another -> findColumnFromSchema(another, arr[1], arr[2]));
            }
        }
        return empty();
    }

    @Override
    public void addForeignKey(ForeignKeyMetadata metadata) {
        foreignKey.add(metadata);

    }

    @Override
    public ForeignKeyMetadata addForeignKey(ForeignKeyBuilder builder) {
        DefaultForeignKeyMetadata foreignKeyMetadata = new DefaultForeignKeyMetadata();
        foreignKeyMetadata.setName(builder.getName());
        foreignKeyMetadata.setAlias(builder.getAlias());
        foreignKeyMetadata.setLogical(true);
        foreignKeyMetadata.setToMany(builder.isToMany());
        foreignKeyMetadata.setTarget(schema.getTableOrView(builder.getTarget())
                .orElseThrow(() -> new IllegalArgumentException("target [" + builder.getTarget() + "] doesn't exist")));

        foreignKeyMetadata.setSourceColumn(getColumn(builder.getSourceColumn())
                .orElseThrow(() -> new IllegalArgumentException("source column [" + builder.getSourceColumn() + "] doesn't exist")));
        foreignKeyMetadata.setTargetColumn(foreignKeyMetadata.getTarget().getColumn(builder.getTargetColumn())
                .orElseThrow(() -> new IllegalArgumentException("target column [" + builder.getTargetColumn() + "] doesn't exist")));
        foreignKeyMetadata.setSource(this);

        foreignKeyMetadata.setTerms(builder.getTerms());
        addForeignKey(foreignKeyMetadata);
        return foreignKeyMetadata;
    }

    private Optional<RDBColumnMetadata> findColumnFromSchema(DefaultRDBSchemaMetadata schema, String tableName, String column) {
        return of(schema.getTableOrView(tableName)
                .flatMap(meta -> meta.getColumn(column)))
                .filter(Optional::isPresent)
                .orElseGet(() -> getForeignKey(tableName) //查找外键关联信息
                        .flatMap(key -> key.getTarget().getColumn(column)));
    }

    @Override
    public List<ForeignKeyMetadata> getForeignKeys() {
        return new ArrayList<>(foreignKey);
    }

    @Override
    public Optional<ForeignKeyMetadata> getForeignKey(String targetName) {
        return foreignKey
                .stream()
                .filter(key -> key.getTarget().equalsNameOrAlias(targetName))
                .findFirst();
    }

    public void registerFeature(Feature feature) {
        features.put(feature.getId(), feature);
    }

    @Override
    public Dialect getDialect() {
        return getSchema()
                .getDialect();
    }
}
