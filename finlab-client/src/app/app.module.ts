import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { API_CONFIG, ApiConfig } from './config/api-config';
import { environment } from '../environments/environment';
import {AuthInterceptor} from "./interceptors/auth.interceptor";
import {HTTP_INTERCEPTORS, HttpClientModule} from "@angular/common/http";
import {LoginComponent} from "./components/auth/login/login.component";
import {IbanLookUpComponent} from "./components/account/iban-lookup/iban-look-up.component";
import {TopbarComponent} from "./components/topbar/topbar.component";
import {StressTestResultsComponent} from "./components/stress-test-results/stress-test-results.component";
import {FormsModule} from "@angular/forms";

@NgModule({
  declarations: [
    AppComponent,
    LoginComponent,
    IbanLookUpComponent,
    TopbarComponent,
    StressTestResultsComponent
  ],
  imports: [
    BrowserModule,
    HttpClientModule,
    AppRoutingModule,
    FormsModule
  ],
  providers: [
    { provide: API_CONFIG, useValue: { baseUrl: environment.apiBaseUrl, apiVersion: environment.apiVersion } as ApiConfig },
    { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true }
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
