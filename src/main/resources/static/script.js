
    var user_id = 0;
    var username = "";

    function formToJson(frm) {
         var result = "{ ";
         var children = frm.getElementsByTagName("input");
         for (var i = 0; i < children.length; i++) {
            if (children[i].hasAttribute("name") && children[i].value) {
                var name = children[i].getAttribute("name");
                if (result.length > 2) result += ", ";
                result += "\"" + name + "\": \"" + children[i].value + "\"";
            }
         }
         children = frm.getElementsByTagName("select");
         for (var i = 0; i < children.length; i++) {
            if (children[i].hasAttribute("name") && children[i].value) {
                var name = children[i].getAttribute("name");
                if (result.length > 2) result += ", ";
                result += "\"" + name + "\": \"" + children[i].value + "\"";
            }
         }
        result += "}";
        return result;
    };

    function findWishlist(e) {
        if (e) {
            e.preventDefault();
        }
        if (user_id != 0) {
            var filter = "?";
            var checkOnlyMy = document.getElementById("checkOnlyMy");
            var inUsername = username;
            if (!checkOnlyMy.checked) {
                inUsername = document.getElementById("inputFilterByUsername").value;
            }
            if (inUsername) {
                filter += "username=" + inUsername + "&";
            }
            var inName = document.getElementById("inputFilterByName");
            if (inputFilterByName.value) {
                filter += "name=" + inputFilterByName.value + "&";
            }
            var orderBy = document.getElementById("selectOrderBy");
            filter += "orderBy=" + orderBy.value;
            var url = "/api/" + user_id + "/wishlist/list" + filter;
            console.log("GET " + url);
            $.ajax({
                type: "GET",
                url: url,
                contentType: "application/json; charset=utf-8",
                success: function(list) {
                    console.log(list);
                    var $table = $("#tableWishlist tbody");
                    $table.empty();
                    var tbody = "";
                    for (i = 0; i < list.length; i++) {
                        tbody += "<tr style='cursor: pointer;' ondblclick='showWishlist(\"" + list[i].id + "\")'>";
                        tbody += "<td>" + list[i].username +"</td>";
                        tbody += "<td>" + list[i].name +"</td>";
                        tbody += "<td>" + (list[i].comment != null ? list[i].comment : "") +"</td>";
                        if (username == list[i].username) {
                            tbody += "<td><button class='btn btn-light' type='button' onclick='deleteWishlist(\"" + list[i].id + "\")'>"+ getTrashIcon() + "</button></td>";
                        } else {
                            tbody += "<td></td>";
                        }
                        tbody += "</tr>";
                    }
                    $table.append(tbody);
                },
                error: function(xhr, status, error) {
                    var errorMessage = xhr.status + ': ' + xhr.statusText;
                    console.log(errorMessage);
                }
            });
        }
    };

    function showWishlist(wishlistId) {
        var url = "/api/" + user_id + "/wishlist/" + wishlistId;
        console.log("GET " + url);
        $.ajax({
                type: "GET",
                url: url,
                contentType: "application/json; charset=utf-8",
                success: function(wishlist) {
                    console.log(wishlist);
                    showModal(wishlist, true, false);
                },
                error: function(xhr, status, error) {
                    var errorMessage = xhr.status + ': ' + xhr.statusText;
                    console.log(errorMessage);
                }
            });
    }

    function login(userId) {
        $("#inUsername").prop('disabled', true);
        $("#butLogin").hide();
        $("#butSign-Up").hide();
        $("#butLogout").show();
        user_id = userId;
        username = $("#inUsername").val();
        $("#tableWishlist tbody").empty();
    }

    function logout() {
        $("#inUsername").prop('disabled', false);
        $("#butLogout").hide();
        $("#butLogin").show();
        $("#butSign-Up").show();
        user_id = 0;
        username = "";
        $("#tableWishlist tbody").empty();
    }

    function showModal(data, showWL, createNew){
        var title = "Wishlist";
        var wishlistId;
        if (!showWL) {
            title = "Wish";
            wishlistId = document.getElementById("inputWLId").value;
            $("#butBack").show();
        } else {
            $("#butBack").hide();
        }
        $('#wishlistModal .modal-body').html("");
        $('#wishlistModal .modal-title').html(title);
        var body = "<form id='modalForm'><div class='form-group'>";
        if (!createNew) {
            $("#butDeleteWL").show();
            if (!showWL) {
                body += "<input id=\"inputWLId\" type=\"text\" value='" + wishlistId + "' hidden>";
                body += "<input id=\"inputWId\" type=\"text\" value='" + data.id + "' hidden>";
            } else {
                body += "<input id=\"inputWLId\" type=\"text\" value='" + data.id + "' hidden>";
            }
        } else {
            $("#butDeleteWL").hide();
            data = [];
            data.name = "";
            data.comment = "";
            data.link = "";
            data.price = "";
            data.status = "Free";
            if (!showWL) {
                body += "<input id=\"inputWLId\" type=\"text\" value='" + wishlistId + "' hidden>";
            }
        }
        body += newInput("inputWLName", "Name", data.name, "name", true);
        if (showWL) {
            body += "<label class=\"col-form-label\" for='selectAccess'>Access</label>"
                        + "<select class=\"form-select\" name=\"access\" id=\"selectAccess\">";
            if (createNew || (data.access == "public")) {
                body += "<option selected>public</option>";
                body += "<option>private</option>";
            } else {
                body += "<option>public</option>";
                body += "<option selected>private</option>";
            }
            body += "</select>";
            body += newInput("inputWLComment", "Comment", data.comment, "comment");
            body += "</div></form>";
        } else {
            body += newInput("inputWLink", "Link", data.link, "link");
            body += newInput("inputWPrice", "Price", data.price, "price");
            body += newInput("inputWLComment", "Comment", data.comment, "comment");
            if (createNew){
                body += "<input id='inputWStatus' type='text' value='free' name='status' hidden>";
            }
            body += "</div></form>";
            if (!createNew){
                body += '<div class="row row-cols-lg-auto g-3 align-items-center">'
                        + '<div class="col-12">';
                body += "<label class=\"col-form-label\" for='selectStatus'>Status</label>"
                            + "<select class=\"form-select\" id=\"selectStatus\">";
                body += "<option" + (data.status == "free" ? " selected" : "") + ">free</option>";
                body += "<option" + (data.status == "booked" ? " selected" : "") + ">booked</option>";
                body += "<option" + (data.status == "shared" ? " selected" : "") + ">shared</option>";
                body += "<option" + (data.status == "got" ? " selected" : "") + ">got</option>";
                body += "</select></div>";
                body += '<div class="col-12"><button onclick="bookWish()" class="btn btn-success" style="margin-top: 36px;">' + getLockIcon() + '</button></div>';
                if (data.status == "shared") {
                    body += '<div class="col-12"><button onclick="addToShare()" class="btn btn-primary" style="margin-top: 36px;">' + getPlusIcon() + '</button></div>';
                    body += '<div class="col-12"><button onclick="removeFromShare()" class="btn btn-danger" style="margin-top: 36px;">' + getMinusIcon() + '</button></div>';
                }
                body += '</div>';
            }
        }
        if (showWL && !createNew) {
            var wishes = data.wishes;
            body += "<table class='table table-hover' id='tableWishes'><thead><th scope='col'>Name</th>" +
                "<th scope='col'>Link</th><th scope='col'>Price</th><th scope='col'>Status</th>" +
                "<th scope='col'>Comment</th></thead><tbody>";
            for (i = 0; i < wishes.length; i++) {
                body += "<tr style='cursor: pointer;' ondblclick='showWish(\"" + data.id + "\", \"" + wishes[i].id + "\")'>";
                body += "<td>" + wishes[i].name +"</td>";
                body += "<td>" + (wishes[i].link != null ? wishes[i].link : "") +"</td>";
                body += "<td>" + (wishes[i].price != null ? wishes[i].price : "") +"</td>";
                body += "<td>" + wishes[i].status +"</td>";
                body += "<td>" + (wishes[i].comment != null ? wishes[i].comment : "") +"</td>";
                body += "</tr>";
            }
            body += "</tbody></table>";
            body += '<button onclick="showModal(null, false, true)" class="btn btn-primary">Add wish</button>';
            body += "<div id='divSubscribers' class='mt-2'>";
            body += "</div>";
        }
        $('#wishlistModal .modal-body').html(body);
        if (showWL && !createNew){
            showSubscribers(data.id);
        }
        $("#wishlistModal").modal("show");
    }

    function showWish(wishlistId, wishId) {
        var url = "/api/" + user_id + "/wishlist/" + wishlistId + "/" + wishId;
        console.log("GET " + url);
        $.ajax({
            type: "GET",
            url: url,
            contentType: "application/json; charset=utf-8",
            success: function(wish) {
                console.log(wish);
                showModal(wish, false, false);
            },
            error: function(xhr, status, error) {
                var errorMessage = xhr.status + ': ' + xhr.statusText;
                console.log(errorMessage);
            }
        });
    }

    function saveWishlist() {
        var frm = document.getElementById("modalForm");
        var inputId = frm.querySelector("#inputWLId");
        var inputWishId = frm.querySelector("#inputWId");
        var isWishlist = $('#wishlistModal .modal-title').html() == "Wishlist";
        var url = "/api/" + user_id + "/wishlist";
        var reqType = "POST";
        if (inputId) {
            if (isWishlist) {
                url += "/" + inputId.value;
                reqType = "PATCH";
            } else {
                if (inputWishId) {
                    url += "/wish/" + inputWishId.value;
                    reqType = "PATCH";
                } else {
                    url += "/" + inputId.value + "/wish";
                    reqType = "POST";
                }
            }
        }
        var data = formToJson(frm);
        console.log(reqType + " " + url);
        $.ajax({
            type: reqType,
            url: url,
            data: data,
            contentType: "application/json; charset=utf-8",
            dataType: "json",
            success: function(wishlist) {
                console.log(wishlist);
                if (isWishlist) {
                    $("#wishlistModal").modal("hide");
                    findWishlist();
                } else {
                    showWishlist(inputId.value);
                }
            },
            error: function(xhr, status, error) {
                var errorMessage = xhr.status + ': ' + xhr.statusText;
                console.log(errorMessage);
            }
        });
    }

    function deleteWL(){
        var frm = document.getElementById("modalForm");
        var inputId = frm.querySelector("#inputWLId");
        var inputWishId = frm.querySelector("#inputWId");
        var isWishlist = $('#wishlistModal .modal-title').html() == "Wishlist";
        if (isWishlist) {
            deleteWishlist(inputId.value);
        } else {
            deleteWish(inputId.value, inputWishId.value);
        }
    }

    function deleteWishlist(wishlistId) {
        var url = "/api/" + user_id + "/wishlist/" + wishlistId;
        console.log("DELETE " + url);
        $.ajax({
            type: "DELETE",
            url: url,
            contentType: "application/json; charset=utf-8",
            success: function() {
                findWishlist();
                $("#wishlistModal").modal("hide");
            },
            error: function(xhr, status, error) {
                var errorMessage = xhr.status + ': ' + xhr.statusText;
                console.log(errorMessage);
            }
        });
    }

    function deleteWish(wishlistId, wishId) {
        var url = "/api/" + user_id + "/wishlist/" + wishlistId + "/" + wishId;
        console.log("DELETE " + url);
        $.ajax({
            type: "DELETE",
            url: url,
            contentType: "application/json; charset=utf-8",
            success: function() {
                showWishlist(inputId.value);
            },
            error: function(xhr, status, error) {
                var errorMessage = xhr.status + ': ' + xhr.statusText;
                console.log(errorMessage);
            }
        });
    }

    function newInput(id, labelName, val, inputName, required, readonly, dataId) {
        var value = "";
        if (val != null){
            value = val;
        }
        var label = "<label for=\"" + id + "\" class=\"col-form-label\">" + labelName + "</label>" +
            "<input type=\"text\" class=\"form-control\" id=\"" + id + "\" value=\"" + value + "\"";
        if (dataId){
            label += " data-id=\"" + dataId + "\"";
        }
        if (required){
            label += " required";
        }
        if (readonly){
            label += " readonly";
        }
        if (inputName){
            label += " name=\"" + inputName + "\"";
        }
        label += ">";
        return label;
    }

    function showSubscribers(wishlistId) {
        var body = '<span>Subscribers: ' +
                '<span style="padding: 2px 2px; border: 1px solid #c6cdd3; display:inline-block;width:85%;" class="ms-1">'
        var url = "/api/" + user_id + "/wishlist/" + wishlistId + "/users";
        console.log("GET " + url);
        $.ajax({
            type: "GET",
            url: url,
            contentType: "application/json; charset=utf-8",
            success: function(users) {
                console.log(users);
                $("#divSubscribers").empty();
                if (users.length > 0) {
                    for (i = 0; i < users.length; i++) {
                        body += "<span class='badge bg-primary m-1 me-0'><span>" + users[i].username + "</span>";
                        body += "<span class='unsubscribe' style='cursor: pointer;' data-username='" + users[i].username + "'>" +
                         getXIcon() + "</span></span></span>";
                    }
                }
                body += '<span class="badge bg-primary m-1" style="cursor: pointer;height: 24.4px;" id="spanAdd">' +
                        '<span>+</span><span>Add</span></span><form style="display:inline-block;" onsubmit="addSubscriber(event)">' +
                        '<input id="inputUserToSubscribe" style="display:none;"></form></span>';
                //body += getAddSubscriberForm();
                $("#divSubscribers").html(body);
                $(".unsubscribe").click(function() {
                    var username = this.getAttribute("data-username");
                    if (username) {
                        unsubscribe(username, this.parentElement);
                    }
                });
                $("#spanAdd").click(function () {
                    $(this).hide();
                    $("#inputUserToSubscribe").show();
                    $("#inputUserToSubscribe").focus();
                });
                $("#inputUserToSubscribe").blur(function() {
                    $("#inputUserToSubscribe").hide();
                    $("#spanAdd").show();
                });
            },
            error: function(xhr, status, error) {
                var errorMessage = xhr.status + ': ' + xhr.statusText;
                console.log(errorMessage);
            }
        });
    }

    function getAddSubscriberForm() {
        return '<div class="row row-cols-lg-auto g-3 align-items-center">'
            + '<div class="col-12"><input type="text" class="form-control" placeholder="Username" id="inSubscriberUsername"></div>'
            + '<div class="col-12"><button type="button" onclick="addSubscriber()" class="btn btn-primary">Subscribe</button></div></div>';
    }

    function addSubscriber(e) {
        e.preventDefault();
        var wishlistId = document.getElementById("inputWLId").value;
        var username = document.getElementById("inputUserToSubscribe").value;
        var url = "/api/" + user_id + "/wishlist/" + wishlistId + "/access?username=" + username;
        console.log("PUT " + url);
        $.ajax({
            type: "PUT",
            url: url,
            contentType: "application/json; charset=utf-8",
            success: function() {
                $("#inputUserToSubscribe").hide();
                $("#spanAdd").show();
                showSubscribers(wishlistId);
            },
            error: function(xhr, status, error) {
                 var errorMessage = xhr.status + ': ' + xhr.statusText;
                 console.log(errorMessage);
            }
        });
    }

    function unsubscribe(username, elem) {
        var frm = document.getElementById("modalForm");
        var inputId = frm.querySelector("#inputWLId");
        var wishlistId = inputId.value;
        var url = "/api/" + user_id + "/wishlist/" + wishlistId + "/access?username=" + username;
        console.log("DELETE " + url);
        $.ajax({
            type: "DELETE",
            url: url,
            contentType: "application/json; charset=utf-8",
            success: function(data) {
                elem.remove();
            },
            error: function(xhr, status, error) {
                var errorMessage = xhr.status + ': ' + xhr.statusText;
                console.log(errorMessage);
            }
        });

    }

    function bookWish() {
        var wishlistId = document.getElementById("inputWLId").value;
        var wishId = document.getElementById("inputWId").value;
        var status = document.getElementById("selectStatus").value;
        var url = "/api/" + user_id + "/wishlist/" + wishlistId + "/wish/" + wishId + "?status=" + status;
        console.log("PATCH " + url)
        $.ajax({
             type: "PATCH",
             url: url,
             contentType: "application/json; charset=utf-8",
             success: function(wish) {
                console.log(wish);
                showModal(wish, false, false);
             },
             error: function(xhr, status, error) {
                  var errorMessage = xhr.status + ': ' + xhr.statusText;
                  console.log(errorMessage);
             }
        });
    }

    function addToShare() {
        var wishlistId = document.getElementById("inputWLId").value;
        var wishId = document.getElementById("inputWId").value;
        var url = "/api/" + user_id + "/wishlist/" + wishlistId + "/wish/" + wishId + "/user";
        console.log("PUT " + url);
        $.ajax({
             type: "PUT",
             url: url,
             contentType: "application/json; charset=utf-8",
             success: function() {
             },
             error: function(xhr, status, error) {
                  var errorMessage = xhr.status + ': ' + xhr.statusText;
                  console.log(errorMessage);
             }
        });
    }

    function removeFromShare() {
        var wishlistId = document.getElementById("inputWLId").value;
        var wishId = document.getElementById("inputWId").value;
        var url = "/api/" + user_id + "/wishlist/" + wishlistId + "/wish/" + wishId + "/user";
        console.log("DELETE " + url);
        $.ajax({
             type: "DELETE",
             url: url,
             contentType: "application/json; charset=utf-8",
             success: function() {
             },
             error: function(xhr, status, error) {
                  var errorMessage = xhr.status + ': ' + xhr.statusText;
                  console.log(errorMessage);
             }
        });
    }

    function getTrashIcon() {
        return '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-trash" viewBox="0 0 16 16">' +
               '<path d="M5.5 5.5A.5.5 0 0 1 6 6v6a.5.5 0 0 1-1 0V6a.5.5 0 0 1 .5-.5zm2.5 0a.5.5 0 0 1 .5.5v6a.5.5 0 0 1-1 0V6a.5.5 0 0 1 .5-.5zm3 .5a.5.5 0 0 0-1 0v6a.5.5 0 0 0 1 0V6z"/>' +
               '<path fill-rule="evenodd" d="M14.5 3a1 1 0 0 1-1 1H13v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V4h-.5a1 1 0 0 1-1-1V2a1 1 0 0 1 1-1H6a1 1 0 0 1 1-1h2a1 1 0 0 1 1 1h3.5a1 1 0 0 1 1 1v1zM4.118 4 4 4.059V13a1 1 0 0 0 1 1h6a1 1 0 0 0 1-1V4.059L11.882 4H4.118zM2.5 3V2h11v1h-11z"/>' +
               '</svg>';
    }

    function getXIcon() {
        return '<span style="cursor: pointer;"><svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-x" viewBox="0 0 16 16">' +
               '<path d="M4.646 4.646a.5.5 0 0 1 .708 0L8 7.293l2.646-2.647a.5.5 0 0 1 .708.708L8.707 8l2.647 2.646a.5.5 0 0 1-.708.708L8 8.707l-2.646 2.647a.5.5 0 0 1-.708-.708L7.293 8 4.646 5.354a.5.5 0 0 1 0-.708z"/>' +
               '</svg>';
    }

    function getLockIcon() {
        return '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-play-fill" viewBox="0 0 16 16">' +
               '<path d="m11.596 8.697-6.363 3.692c-.54.313-1.233-.066-1.233-.697V4.308c0-.63.692-1.01 1.233-.696l6.363 3.692a.802.802 0 0 1 0 1.393z"/>' +
               '</svg>';
    }

    function getPlusIcon() {
        return '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-plus-lg" viewBox="0 0 16 16">' +
               '<path d="M8 0a1 1 0 0 1 1 1v6h6a1 1 0 1 1 0 2H9v6a1 1 0 1 1-2 0V9H1a1 1 0 0 1 0-2h6V1a1 1 0 0 1 1-1z"/>' +
               '</svg>';
    }

    function getMinusIcon() {
        return '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-dash-lg" viewBox="0 0 16 16">' +
               '<path d="M0 8a1 1 0 0 1 1-1h14a1 1 0 1 1 0 2H1a1 1 0 0 1-1-1z"/>' +
               '</svg>';
    }