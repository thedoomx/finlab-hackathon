import {Component, OnDestroy} from '@angular/core';
import { AccountService } from "../../../services/account.service";
import { TokenService } from "../../../services/token.service";
import {Subscription} from "rxjs";

interface LookupResult {
  iban: string;
  status: string;
  timestamp: Date;
}

@Component({
  selector: 'app-iban-lookup',
  templateUrl: './iban-look-up.component.html',
  styleUrls: ['./iban-look-up.component.css']
})
export class IbanLookUpComponent implements OnDestroy {
  iban: string = '';
  error: string | null = null;
  loading: boolean = false;
  history: LookupResult[] = [];
  private tokenWatcher?: Subscription;

  constructor(
    private accountService: AccountService,
    public tokenService: TokenService
  ) {
    this.tokenWatcher = this.tokenService.tokenCleared$.subscribe(() => {
      this.clearHistory();
    });
  }

  lookup() {
    if (!this.iban || this.iban.trim().length === 0) {
      this.error = 'Please enter an IBAN';
      return;
    }

    this.error = null;
    this.loading = true;

    this.accountService.ibanLookup(this.iban.trim()).subscribe({
      next: status => {
        debugger;
        this.history.unshift({
          iban: this.iban.trim(),
          status: status,
          timestamp: new Date()
        });
        this.loading = false;
        this.iban = '';
      },
      error: err => {
        debugger;

        this.error = 'Failed to lookup IBAN';
        this.loading = false;
      }
    });
  }

  clearHistory() {
    this.history = [];
  }

  getStatusClass(status: string): string {
    switch (status.toUpperCase()) {
      case 'ALLOW':
        return 'status-allow';
      case 'REVIEW':
        return 'status-review';
      case 'BLOCK':
        return 'status-block';
      default:
        return '';
    }
  }

  ngOnDestroy() {
    this.tokenWatcher?.unsubscribe();
  }
}
