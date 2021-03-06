package org.xiaochao.tool;

import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import cn.hutool.setting.Setting;
import cn.hutool.setting.SettingUtil;
import org.xiaochao.model.GridModel;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.xiaochao.model.GridModel.createOneGrid;

/**
 * 网格交易1.0版本 最简单的策略 不留利润 不逐层加码
 *
 * @Author jiang_ruixin
 * @Date 2019/6/25 21:00
 **/
public class Grid {
    private static final String GENERATE_FILE_DIR;
    private static final String FILE_NAME;
    private static final double PER_GRID;
    private static final double MAX_LOSS;

    private static final double CURRENT_PRICE;
    private static final double MAX_GRID_PRICE;

    static {
        Setting setting = SettingUtil.get("grid.properties");
        GENERATE_FILE_DIR = setting.getStr("generate_file_dir");
        FILE_NAME = setting.getStr("file_name");
        PER_GRID = setting.getDouble("per_grid");
        MAX_LOSS = setting.getDouble("max_loss");
        CURRENT_PRICE = setting.getDouble("current_price");
        MAX_GRID_PRICE = setting.getDouble("max_grid_price");
    }

    public static void main(String[] args) {
        Grid grid = new Grid();
        grid.grid();

    }

    public void grid() {
        List<GridModel> gridModels = gen();
        write2Excel(gridModels);
    }


    private List<GridModel> gen() {
        List<GridModel> gridModels = genPricesGtCurrentPrice();
        gridModels.addAll(genPricesLteCurrentPrice());
        gridModels.add(genSum(gridModels));
        return gridModels;

    }

    /**
     * 生成大于当前价格的网格
     *
     * @return
     */
    private List<GridModel> genPricesGtCurrentPrice() {
        List<GridModel> gridModels = new ArrayList<>();
        double nextBuyPrice;
        int level = 1;
        do {
            double gridBuyLevel = 1.0 + PER_GRID * level / 100;
            double gridSellLevel = 1.0 + PER_GRID * (level + 1) / 100;
            nextBuyPrice = CURRENT_PRICE * gridBuyLevel;
            double nextSellPrice = CURRENT_PRICE * gridSellLevel;
            level++;
            gridModels.add(createOneGrid(nextBuyPrice, nextSellPrice, gridBuyLevel));
        } while (nextBuyPrice < MAX_GRID_PRICE);
        gridModels.sort(Comparator.comparingDouble(GridModel::getLevel).reversed());
        return gridModels;
    }

    /**
     * 生成小于等于当前价格的网格
     *
     * @return
     */
    private List<GridModel> genPricesLteCurrentPrice() {
        List<GridModel> gridModels = new ArrayList<>();
        int grids = (int) (MAX_LOSS / PER_GRID);
        for (int i = 0; i < grids; i++) {
            double gridBuyLevel = 1.0 - PER_GRID * i / 100;
            double gridSellLevel = 1.0 - PER_GRID * (i - 1) / 100;
            double buyPrice = CURRENT_PRICE * gridBuyLevel;
            double sellPrice = CURRENT_PRICE * gridSellLevel;
            gridModels.add(createOneGrid(buyPrice, sellPrice, gridBuyLevel));
        }
        return gridModels;
    }

    /**
     * 生成合计栏
     *
     * @param gridModels
     * @return
     */
    private GridModel genSum(List<GridModel> gridModels) {
        GridModel gridModel = new GridModel();
        gridModel.setBuyNum(gridModels.stream().mapToInt(GridModel::getBuyNum).sum());
        gridModel.setBuyPriceSum(gridModels.stream().mapToDouble(GridModel::getBuyPriceSum).sum());
        gridModel.setSellNum(gridModels.stream().mapToInt(GridModel::getSellNum).sum());
        gridModel.setSellPriceSum(gridModels.stream().mapToDouble(GridModel::getSellPriceSum).sum());
        gridModel.setProfit(gridModels.stream().mapToDouble(GridModel::getProfit).sum());
        gridModel.setProfitPercentage(gridModel.getProfit() / gridModel.getBuyPriceSum() * 100);
        return gridModel;
    }


    private void write2Excel(List<GridModel> gridModels) {
        ExcelWriter writer = ExcelUtil.getWriter(GENERATE_FILE_DIR + File.separator + FILE_NAME + ".xlsx");
        writer.addHeaderAlias("level", "与基准比较");
        writer.addHeaderAlias("buyPrice", "买入价格");
        writer.addHeaderAlias("buyNum", "买入数量");
        writer.addHeaderAlias("buyPriceSum", "买入价格合计");
        writer.addHeaderAlias("sellPrice", "卖出价格");
        writer.addHeaderAlias("sellNum", "卖出数量");
        writer.addHeaderAlias("sellPriceSum", "卖出价格合计");
        writer.addHeaderAlias("leftNum", "留存数量");
        writer.addHeaderAlias("leftProfitSellPrice", "留存售出价格");
        writer.addHeaderAlias("profit", "盈利");
        writer.addHeaderAlias("profitPercentage", "盈利百分比");
        writer.write(gridModels);
        writer.setColumnWidth(0, 20);
        writer.setColumnWidth(1, 20);
        writer.setColumnWidth(2, 20);
        writer.setColumnWidth(3, 20);
        writer.setColumnWidth(4, 20);
        writer.setColumnWidth(5, 20);
        writer.setColumnWidth(6, 20);
        writer.setColumnWidth(7, 20);
        writer.setColumnWidth(8, 20);
        writer.setColumnWidth(9, 20);
        writer.setColumnWidth(10, 20);
        writer.flush();
        writer.close();
    }

}
