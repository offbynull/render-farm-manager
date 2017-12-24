import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { ActorTestComponent } from './actor-test.component';
import { ActorService } from './actor.service';

@NgModule({
  imports: [
    CommonModule, FormsModule
  ],
  declarations: [ActorTestComponent],
  exports: [ActorTestComponent],
  providers: [ActorService],
})
export class ActorModule { }
