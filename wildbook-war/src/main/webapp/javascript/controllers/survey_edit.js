wildbook.app.controller("SurveyEditController", function($scope, $http, $exceptionHandler, wbConfig) {
    var panelName = "survey_edit";
    $scope.info = {};

    $scope.$on(panelName, function(event, data) {
        if (data) {
            $scope.data = data;
        } else {
//            // create empty new survey with one track.
//            $scope.data = {tracks: [{}]};
            $scope.data = {};
        }
    });

    $scope.orgChange = function() {
        //
        // This is apparently a copy of the object in the collection so
        // setting anything on this is not preserved from one selection
        // to the next. So we have to adjust the original collection.
        //
        var org = $scope.data.survey.organization;

        if (org == null) {
            $scope.info.vessels = null;
            delete $scope.data.survey.organization;
            return;
        }

        wbConfig.getVessels(org)
        .then(function(vessels) {
            $scope.info.vessels = vessels;
        });
    };

    $scope.save = function() {
        $http.post('obj/survey/savetrack', $scope.data)
        .then(function() {
            $scope.panels[panelName] = false;
            $scope.$emit(panelName + "_done", $scope.data);
        }, $exceptionHandler);
    };
    
    //
    // wb-key-handler-form
    //
    $scope.cancel = function() {
        $scope.data = null;
        $scope.panels[panelName] = false;
    }
    
    $scope.cmdEnter = function() {
        $scope.save();
    }
}); 
