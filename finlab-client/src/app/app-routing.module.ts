import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import {LoginComponent} from "./components/auth/login/login.component";
import {IbanLookUpComponent} from "./components/account/iban-lookup/iban-look-up.component";
import {StressTestResultsComponent} from "./components/stress-test-results/stress-test-results.component";

const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'accounts', component: IbanLookUpComponent },
  { path: 'test-results', component: StressTestResultsComponent },
  { path: '**', redirectTo: 'login' }
];
@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
