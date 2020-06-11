package com.codingdm.mriya.mysql.parser;

import com.alibaba.druid.sql.ast.statement.SQLColumnDefinition;
import com.alibaba.druid.sql.ast.statement.SQLSelectOrderByItem;
import com.alibaba.druid.sql.dialect.mysql.ast.MySqlPrimaryKey;
import com.codingdm.mriya.AlterTable;
import com.codingdm.mriya.enums.AlterTypeEnum;
import com.codingdm.mriya.model.Column;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author wudongming1
 * @email dongming1.wu@genscript.com
 * @Date 5/29/2020 11:04 AM
 **/
@Slf4j
@Data
public class MysqlParser implements AlterTable {

    private List<Column> columns;

    public MysqlParser(){

        columns = new ArrayList<>();
    }

    @Override
    public void createTable(List<SQLColumnDefinition> definitions) {
        setColumns(definitions.stream()
                .map(MysqlParser::beanConvert)
                .peek(c->c.setAlterType(AlterTypeEnum.CREATE))
                .collect(Collectors.toList())
        );
    }

    @Override
    public void addColumns(List<SQLColumnDefinition> definitions) {
        this.columns.addAll(definitions.stream()
                .map(MysqlParser::beanConvert)
                .peek(c->c.setAlterType(AlterTypeEnum.ADD))
                .collect(Collectors.toList()));
    }

    @Override
    public void dropColumns(String columnName) {
        Column column = new Column(columnName);
        column.setAlterType(AlterTypeEnum.DROP);
        this.columns.add(column);
    }

    @Override
    public void modifyColumn(SQLColumnDefinition definition) {
        Column column = MysqlParser.beanConvert(definition);
        column.setAlterType(AlterTypeEnum.MODIFY);
        this.columns.add(column);
    }

    @Override
    public void changeColumn(SQLColumnDefinition definition, String oldName) {
        Column column = MysqlParser.beanConvert(definition);
        column.setOldName(oldName);
        column.setAlterType(AlterTypeEnum.CHANGE);
        this.columns.add(column);
    }

    @Override
    public void setPrivateKeys(MySqlPrimaryKey privateKey) {
        for (SQLSelectOrderByItem item : privateKey.getColumns()) {
            for (int i = 0; i < this.columns.size(); i++) {
                if(cleanColumnName(item.getExpr().toString())
                        .equals(cleanColumnName(this.getColumns().get(i).getName()))){
                    this.columns.get(i).setIsPrivateKey(true);
                }
            }
        }
    }


    /**
     * SQLColumnDefinition -> Column
     * @param definition
     * @return
     */
    public static Column beanConvert(SQLColumnDefinition definition) {
        Column column = new Column();
        column.setName(definition.getName().getSimpleName());
        column.setType(definition.getDataType().getName());
        if(!Objects.isNull(definition.getComment())){
            column.setComment(definition.getComment().toString());
        }
        column.setIsPrivateKey(definition.isPrimaryKey());
        if(definition.getDataType().getArguments().size() > 0){
            column.setLength(Integer.parseInt(definition.getDataType().getArguments().get(0).toString()));
        }
        if(definition.getDataType().getArguments().size() > 1){
            column.setNumericScale(Integer.parseInt(definition.getDataType().getArguments().get(1).toString()));
        }
        return column;
    }

    public static String cleanColumnName(String originalName){
        return originalName.replaceAll("`*?`", "")
                .replaceAll(".*[.]", "");
    }

}