/**
 * Created by cquiros on 4/26/17.
 */

$(document).ready(function() {
    $('.easy-pie-chart .number.transactions').easyPieChart({
                animate: 1000,
                size: 75,
                lineWidth: 3,
                barColor: App.getBrandColor('yellow')
            });
});