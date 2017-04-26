app.controller("folderController", function($scope, $rootScope, $http, $timeout, folderService) {
	
	$scope.display = "table";
	$scope.images = [];
	$scope.folder = false;
	
	$rootScope.$on("openFolder", function(event, data) {
		$scope.display = 'table';
		$scope.images = [];
		$scope.folder = true;
		
		$http.get("/datamanager/content?path=" + encodeURIComponent(data.path) + "&withFile=true").then(function(response) {
			$scope.foldercontent = response.data.folders;
			$scope.foldercontent = $scope.foldercontent.concat(response.data.files);
			$scope.folder = response.data;
		});
	});
	
	$scope.order = function (col) {
		$scope.folderOrderBy = col;
	}
	
	$scope.delete = function (data) {
		if ($rootScope.user.admin) {
			$http.delete("/datamanager/file?path=" + encodeURIComponent(data.path)).then(function(response) {
				$scope.refresh();
			});
		}
	}
	
	$scope.push = function (data) {
		if ($rootScope.user.admin && (data.state != 'REMOTE')) {
			$http.post("/datamanager/acd?path=" + encodeURIComponent(data.path)).then(function(response) {
				$scope.refresh();
			});
		}
	}
	
	$scope.canDelete = function (data) {
		return $rootScope.user.admin && !data.files;
	}
	
	$scope.canUpload = function () {
		return $rootScope.user.admin
	}
	
	$scope.canDownload = function (data) {
		return !data.files;
	}
	
	$scope.getIcon = function (data) {
		folderService.getIcon(data);
	}
	
	$scope.isFolder = function (data) {
		if (data) {
			return data.files;
		}
	}
	
	$scope.canPreview = function (data) {
		if (data) {
			return !data.files;
		}
	}
	
	$scope.getUrl = function (data) {
		if (data) {
			return "/datamanager/file?path=" + encodeURIComponent(data.path);
		}
	}
	
	$scope.downloadUrl = function (data) {
		if (data) {
			return "/datamanager/download?path=" + encodeURIComponent(data.path);
		}
	}
	
	$scope.preview = function (data) {
		$scope.selectedRow = data;
	}
	
	$scope.openFolder = function(data) {
		if (data.files) {
			$rootScope.$broadcast('openFolder', data);
		}
	}
	
	$scope.changeDisplay = function (newDisplay) {
		$scope.display = newDisplay;
		var images = []
		var files = $scope.folder.files;
		for (i=0;i<files.length;i++) {
			if (files[i].name.endsWith('jpg')) {
				images.push(files[i]);
			}
		}
		$scope.images = images;
		
		$timeout(function() {
	          $('.carousel-indicators li').first().addClass('active');
	          $('.carousel-inner .item').first().addClass('active');
	        });
	}
	
    $scope.refresh = function () {
    	$scope.openFolder($scope.folder);
    }
    
    $rootScope.$on('refreshData', function(evt, path) {
    	$scope.openFolder(path);
	});
});