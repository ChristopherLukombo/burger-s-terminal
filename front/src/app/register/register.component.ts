import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { Component, Input, OnInit, Injector } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatDialog } from '@angular/material';
import { TranslateService } from '@ngx-translate/core';
import { NGXLogger } from 'ngx-logger';
import { environment } from '../../environments/environment';
import { User } from '../../model/model.user';
import { AppConstants } from '../app.constants';
import { DialogSuccessComponent } from '../dialog-success/dialog-success.component';
import { RoleName } from '../RoleName';
import { AuthProviderService } from '../services/auth-provider.service';
import { ServicesDataService } from '../services/services-data.service';

export interface RoleView {
    value: string;
    viewValue: string;
}

const WIDTH = '50%';
const HEIGHT = '15%';

@Component({
    selector: 'app-register',
    templateUrl: './register.component.html',
    styleUrls: ['./register.component.css']
})
export class RegisterComponent implements OnInit {

    user: User;
    registerForm: FormGroup;
    submitted = false;
    startDate = new Date(1990, 0, 1);

    selectedFiles: FileList;
    currentFileUpload: File;
    @Input() image: string;
    errorMessage: string;

    roles: RoleView[];

    isAdmin: boolean;

    constructor(
        private servicesDataService: ServicesDataService,
        private logger: NGXLogger,
        // private translateService: TranslateService,
        protected injector: Injector,
        public authProviderService: AuthProviderService,
        private formBuilder: FormBuilder,
        public dialog: MatDialog,
    ) { }

    private static checkExtension(file: File): boolean {
        if (!file) {
            return false;
        }
        const extensions = [
            'image/jpeg',
            'image/png',
            'image/gif',
            'image/tif'
        ];
        return -1 !== extensions.indexOf(file.type);
    }

    ngOnInit() {
        this.createForm();
        this.roles = this.getRoles();
        this.isAdmin = this.authProviderService.isAdmin();
    }

    private createForm() {
         const target = {
            pseudo: ['', [Validators.required, Validators.maxLength(50)]],
            firstName: ['', [Validators.required, Validators.maxLength(50)]],
            lastName: ['', [Validators.required, Validators.maxLength(50)]],
            email: ['', [Validators.required, Validators.minLength(5), Validators.maxLength(100), Validators.email]],
            password: ['', [Validators.required, Validators.minLength(12), Validators.maxLength(20)]],
            imageUrl: ['', []],
            birthDay: ['', [Validators.required]]
        };
        if (this.authProviderService.isAuthenticated() &&
            this.authProviderService.isAdmin()) {
            const source = {
                role: ['', [Validators.required]]
            };
            this.registerForm = this.formBuilder.group(Object.assign(target, source));
        } else {
            this.registerForm = this.formBuilder.group(target);
        }
    }

    private getRoles() {
        const roleList = [];
        RoleName.ROLES.forEach(function (role) {
            roleList.push({
                value: role.id,
                viewValue: role.name.substring(5, role.name.length)
            });
        });
        return roleList;
    }

    // convenience getter for easy access to form fields
    get f() { return this.registerForm.controls; }

    public registerUser(): void {
        this.submitted = true;
        // stop here if form is invalid
        if (this.registerForm.invalid) {
            return;
        }
        this.setUser();
        this.initFile();
        if (this.currentFileUpload) {
            if (!RegisterComponent.checkExtension(this.currentFileUpload)) {
                this.errorMessage = 'Fichier non valide';
                return;
            }
        }
        if (!environment.production) {
            this.logger.debug(AppConstants.CALL_SERVICE, this.user);
        }
        this.servicesDataService.save(this.user, this.injector.get(TranslateService).currentLang)
            .subscribe(data => {
                this.logger.info(AppConstants.USER_SAVED_SUCCESSFULLY);
                this.handleSuccessRegister(getUserId, data);
            }, error => {
                this.logger.error(AppConstants.USER_HASNT_BEEN_SAVED, error.message, error.status);
                this.handleErrorRegister(error);
            });

        function getUserId(data) {
            let userId: number;
            if ('/api/users/' === data.headers.get('Location').slice(0, 11)) {
                userId = data.headers.get('Location').slice(11, data.headers.get('Location').length);
            }
            return userId;
        }
    }

    private initFile() {
        if (this.selectedFiles) {
            this.currentFileUpload = this.selectedFiles.item(0);
        }
    }

    private handleSuccessRegister(getUserId: (data: any) => number, data: HttpResponse<Object>) {
        if (this.currentFileUpload) {
            const userId = getUserId(data);
            this.upload(userId);
        }
        this.resetForm();
        this.errorMessage = undefined;
        this.openDialogSuccess();
    }

    private handleErrorRegister(error: any) {
        if (error instanceof HttpErrorResponse) {
            if (422 === error.status) {
                Object.keys(error.error).forEach(prop => {
                    const formControl = this.registerForm.get(prop);
                    if (formControl) {
                        formControl.setErrors({
                            serverError: error.error[prop]
                        });
                    }
                });
            } else if (400 === error.status) {
                this.errorMessage = error.error;
            } else if (500 === error.status) {
                this.errorMessage = 'Une erreur serveur s\'est produite';
            }
        }
    }

    private resetForm() {
        this.registerForm.reset();
        for (const key in this.registerForm.controls) {
            if (!this.registerForm.controls[key]) {
                continue;
            }
            this.registerForm.controls[key].setErrors(null);
        }
    }

    private setUser() {
        this.user = new User();
        this.user.pseudo = this.registerForm.get('pseudo').value;
        this.user.firstName = this.registerForm.get('firstName').value;
        this.user.lastName = this.registerForm.get('lastName').value;
        this.user.email = this.registerForm.get('email').value;
        this.user.password = this.registerForm.get('password').value;
        this.user.imageUrl = this.registerForm.get('imageUrl').value;
        this.user.birthDay = this.registerForm.get('birthDay').value;
        this.user.roleId = this.registerForm.get('role') ? this.registerForm.get('role').value : RoleName.ROLES[0].id;
        this.user.activated = true;
        this.user.createDate = new Date();
    }

    selectFile(event) {
        this.selectedFiles = event.target.files;
    }

    private upload(userId: number) {
        this.servicesDataService.uploadFile(this.currentFileUpload, userId)
            .subscribe((event) => {
                if (event instanceof HttpResponse) {
                    this.logger.debug('File is completely uploaded!');
                }
            });
        this.selectedFiles = undefined;
    }

    public openDialogSuccess(): void {
        this.dialog.open(
            DialogSuccessComponent,
            {
                width: WIDTH,
                height: HEIGHT
            }
        );
    }
}